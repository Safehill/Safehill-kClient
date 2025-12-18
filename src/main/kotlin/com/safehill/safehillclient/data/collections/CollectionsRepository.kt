package com.safehill.safehillclient.data.collections

import com.safehill.kclient.models.dtos.collections.CheckoutSessionDTO
import com.safehill.kclient.models.dtos.collections.CheckoutSessionUIMode
import com.safehill.kclient.models.dtos.collections.CollectionOutputDTO
import com.safehill.kclient.models.dtos.collections.CollectionVisibility
import com.safehill.kclient.models.dtos.collections.CreateCheckoutSessionRequestDTO
import com.safehill.kclient.models.dtos.collections.IAPReceiptValidationRequestDTO
import com.safehill.kclient.models.dtos.collections.IAPReceiptValidationResponseDTO
import com.safehill.kclient.models.dtos.collections.PriceRangeDTO
import com.safehill.kclient.models.dtos.collections.SearchScope
import com.safehill.kclient.models.dtos.websockets.CollectionChanged
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.util.isSafehillHttpNotFound
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.kclient.util.safeApiCall
import com.safehill.safehillclient.SafehillClient
import com.safehill.safehillclient.data.collections.mappers.toCollection
import com.safehill.safehillclient.data.collections.mappers.toCollectionAccess
import com.safehill.safehillclient.data.collections.model.CollectionAccess
import com.safehill.safehillclient.data.collections.model.CollectionModel
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.config.ClientOptions
import com.safehill.safehillclient.utils.api.dispatchers.SdkDispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionsRepository(
    clientOptions: ClientOptions,
    private val webSocketApi: WebSocketApi,
    private val serverProxy: ServerProxy,
    private val sdkDispatchers: SdkDispatchers,
) : UserObserver {

    private val userScope = clientOptions.userScope
    private val safehillLogger = clientOptions.safehillLogger

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)

    private val _allCollections = MutableStateFlow<Map<String, CollectionModel>>(emptyMap())
    val allCollections = _allCollections.asStateFlow()

    private val _topPickIds = MutableStateFlow<Set<String>>(emptySet())

    private val _ownedAndAccessedCollectionIds = MutableStateFlow<Set<String>>(emptySet())


    val topPicks: StateFlow<List<CollectionModel>> = combine(
        _allCollections,
        _topPickIds
    ) { collections, topPickIds ->
        topPickIds.mapNotNull { collections[it] }
    }.stateIn(clientOptions.clientScope, SharingStarted.Eagerly, emptyList())

    val ownedCollections: StateFlow<List<CollectionModel>> = combine(
        _allCollections,
        _ownedAndAccessedCollectionIds,
        _currentUserId
    ) { collections, ownedAndAccessedIds, userId ->
        userId?.let { userId ->
            ownedAndAccessedIds
                .mapNotNull { collections[it] }
                .filter { it.isOwnedBy(userId) }
        } ?: emptyList()
    }.stateIn(clientOptions.clientScope, SharingStarted.Eagerly, emptyList())

    val accessedCollections: StateFlow<List<CollectionModel>> = combine(
        _allCollections,
        _ownedAndAccessedCollectionIds,
        _currentUserId
    ) { collections, ownedAndAccessedIds, userId ->
        userId?.let { userId ->
            ownedAndAccessedIds
                .mapNotNull { collections[it] }
                .filterNot { it.isOwnedBy(userId) }
        } ?: emptyList()
    }.stateIn(clientOptions.clientScope, SharingStarted.Eagerly, emptyList())


    suspend fun refreshCollections(): Result<Unit> {
        return runCatchingSafe {
            withLoading {
                val (ownedAndAccessedCollections, topPickCollections) = coroutineScope {
                    val ownedAndAccessedDeferred = async {
                        fetchOwnedAndAccessedCollections()
                    }
                    val topPicksDeferred = async {
                        fetchTopPicks()
                    }

                    ownedAndAccessedDeferred.await() to topPicksDeferred.await()
                }

                val allCollectionsMap = (ownedAndAccessedCollections + topPickCollections)

                _ownedAndAccessedCollectionIds.update { ownedAndAccessedCollections.keys }
                _topPickIds.update { topPickCollections.keys }
                _allCollections.update { allCollectionsMap }
            }
        }
    }

    private suspend fun fetchOwnedAndAccessedCollections(): Map<String, CollectionModel> {
        val allResult = safeApiCall {
            serverProxy.remoteServer.retrieveCollections()
        }

        return allResult.getOrElse { error ->
            safehillLogger.error("Failed to fetch owned and accessed collections. $error")
            throw error
        }.associate { dto ->
            val collection = dto.toCollection()
            collection.id to collection
        }
    }

    private suspend fun fetchTopPicks(): Map<String, CollectionModel> {
        val topPicksResult = safeApiCall {
            serverProxy.remoteServer.topPickCollections()
        }

        return topPicksResult.getOrElse { error ->
            safehillLogger.error("Failed to fetch top picks. $error")
            throw error
        }.associate { dto ->
            val collection = dto.toCollection()
            collection.id to collection
        }
    }

    private fun CollectionOutputDTO.toCollection(): CollectionModel {
        val existingCollection = _allCollections.value[this.id]
        return this.toCollection(
            existingCollection?.access ?: CollectionAccess.Unknown
        )
    }


    private fun findCollectionInCache(id: String): CollectionModel? {
        return _allCollections.value[id]
    }

    private suspend fun getCollectionWithPurchaseStatus(collectionID: String): Result<CollectionModel> {
        return runCatchingSafe {
            val dto = serverProxy.remoteServer.retrieveCollection(collectionID)
            val access = getCollectionAccess(dto.id).getOrThrow()
            dto.toCollection(access = access)
        }
    }

    suspend fun trackCollectionAccess(id: String): Result<Unit> {
        return safeApiCall {
            serverProxy.remoteServer.trackCollectionAccess(id)
        }
    }

    /**
     * Search for collections
     */
    suspend fun searchCollections(
        query: String?,
        searchScope: SearchScope = SearchScope.All,
        visibility: CollectionVisibility? = null,
        priceRange: PriceRangeDTO? = null
    ): Result<List<CollectionModel>> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.searchCollections(
                    query = query,
                    searchScope = searchScope,
                    visibility = visibility,
                    priceRange = priceRange
                ).map { it.toCollection() }
            }
        }
    }


    private fun updateCollectionAccessInCache(id: String, access: CollectionAccess) {
        _allCollections.update {
            val existing = it[id]
            if (existing != null) {
                it + (id to existing.copy(access = access))
            } else {
                it
            }
        }
    }

    private suspend fun <T> withLoading(block: suspend () -> T): T {
        return try {
            _loading.update { true }
            block()
        } finally {
            _loading.update { false }
        }
    }

    /**
     * Create a checkout session for purchasing collection access
     */
    suspend fun createCheckoutSession(
        collectionId: String,
    ): Result<CheckoutSessionDTO> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.createCheckoutSession(
                    collectionId,
                    CreateCheckoutSessionRequestDTO(
                        uiMode = CheckoutSessionUIMode.HOSTED,
                    )
                )
            }
        }
    }

    /**
     * Validate an in-app purchase receipt
     */
    suspend fun validateIAPReceipt(
        collectionId: String,
        request: IAPReceiptValidationRequestDTO
    ): Result<IAPReceiptValidationResponseDTO> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.validateIAPReceipt(collectionId, request)
            }.also { result ->
                result.onSuccess { response ->
                    if (response.success) {
                        // Refresh collections to reflect payment
                        refreshCollection(collectionId)
                    }
                }
            }
        }
    }


    suspend fun getCollectionAccess(collectionId: String): Result<CollectionAccess> {
        return safeApiCall {
            serverProxy
                .remoteServer
                .checkCollectionAccess(collectionId)
                .toCollectionAccess()
                .also { collectionAccess ->
                    updateCollectionAccessInCache(collectionId, collectionAccess)
                }
        }
    }


    suspend fun refreshCollection(id: String): Result<CollectionModel> {
        val result = getCollectionWithPurchaseStatus(collectionID = id)
        result.onSuccess { collectionModel ->
            if (collectionModel.isArchived) {
                removeCollectionFromCache(id)
            } else {
                updateCollectionInCache(collectionModel)
            }
        }.onFailure {
            if (it.isSafehillHttpNotFound()) {
                removeCollectionFromCache(id)
            }
        }
        return result
    }


    private fun updateCollectionInCache(collection: CollectionModel) {
        _allCollections.update { it + (collection.id to collection) }
    }

    private fun removeCollectionFromCache(id: String) {
        _allCollections.update { it - id }
        _topPickIds.update { it - id }
        _ownedAndAccessedCollectionIds.update { it - id }
    }

    override suspend fun userLoggedIn(user: LocalUser) {
        _currentUserId.update { user.identifier }
        startListeningToCollectionSocketEvents()
        userScope.launch {
            refreshCollections()
        }
    }

    private fun startListeningToCollectionSocketEvents() {
        userScope.launch {
            webSocketApi.socketMessages
                .filterIsInstance<CollectionChanged>()
                .collect { collectionChanged ->
                    refreshCollection(collectionChanged.collectionId)
                }
        }
    }

    override fun userLoggedOut() {
        _currentUserId.update { null }
        _allCollections.update { mapOf() }
        _topPickIds.update { setOf() }
        _ownedAndAccessedCollectionIds.update { setOf() }
    }
}

val SafehillClient.collectionsRepository
    get() = this.repositories.collectionsRepository
