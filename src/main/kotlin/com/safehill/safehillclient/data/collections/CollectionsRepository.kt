package com.safehill.safehillclient.data.collections

import com.safehill.kclient.models.dtos.collections.AccessCheckResultDTO
import com.safehill.kclient.models.dtos.collections.CheckoutSessionDTO
import com.safehill.kclient.models.dtos.collections.CollectionAssetAddRequestDTO
import com.safehill.kclient.models.dtos.collections.CollectionAssetAddResultDTO
import com.safehill.kclient.models.dtos.collections.CollectionAssetCopyRequestDTO
import com.safehill.kclient.models.dtos.collections.CollectionAssetCopyResultDTO
import com.safehill.kclient.models.dtos.collections.CollectionChangeVisibilityRequestDTO
import com.safehill.kclient.models.dtos.collections.CollectionChangeVisibilityResultDTO
import com.safehill.kclient.models.dtos.collections.CollectionVisibility
import com.safehill.kclient.models.dtos.collections.CreateCheckoutSessionRequestDTO
import com.safehill.kclient.models.dtos.collections.IAPReceiptValidationRequestDTO
import com.safehill.kclient.models.dtos.collections.IAPReceiptValidationResponseDTO
import com.safehill.kclient.models.dtos.collections.PriceRangeDTO
import com.safehill.kclient.models.dtos.collections.SearchScope
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.kclient.util.safeApiCall
import com.safehill.safehillclient.SafehillClient
import com.safehill.safehillclient.data.collections.model.CollectionModel
import com.safehill.safehillclient.data.collections.model.toCollection
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.config.ClientOptions
import com.safehill.safehillclient.utils.api.dispatchers.SdkDispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionsRepository(
    private val serverProxy: ServerProxy,
    private val sdkDispatchers: SdkDispatchers,
    private val clientOptions: ClientOptions
) : UserObserver {

    private val userScope = clientOptions.userScope
    private val safehillLogger = clientOptions.safehillLogger

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)

    private val _allCollections = MutableStateFlow<List<CollectionModel>>(emptyList())
    val allCollections: StateFlow<List<CollectionModel>> = _allCollections.asStateFlow()

    private val _topPicks = MutableStateFlow<List<CollectionModel>>(emptyList())
    val topPicks: StateFlow<List<CollectionModel>> = _topPicks.asStateFlow()

    val ownedCollections: StateFlow<List<CollectionModel>> = combine(
        allCollections,
        _currentUserId
    ) { collections, userId ->
        if (userId == null) {
            emptyList()
        } else {
            collections.filter { it.isOwnedBy(userId) }
        }
    }.stateIn(clientOptions.clientScope, SharingStarted.Eagerly, emptyList())

    val accessedCollections: StateFlow<List<CollectionModel>> = combine(
        allCollections,
        _currentUserId
    ) { collections, userId ->
        if (userId == null) {
            emptyList()
        } else {
            collections.filter { !it.isOwnedBy(userId) }
        }
    }.stateIn(clientOptions.clientScope, SharingStarted.Eagerly, emptyList())

    /**
     * Refresh all collections from the server
     */
    suspend fun refreshCollections(): Result<Unit> {
        return runCatchingSafe {
            coroutineScope {
                launch {
                    refreshAllCollections()
                }
                launch {
                    refreshTopPicks()
                }
            }
        }
    }

    private suspend fun refreshAllCollections() {
        val allResult = safeApiCall {
            serverProxy.remoteServer.retrieveCollections()
        }

        allResult.onSuccess { dtos ->
            val collections = dtos.map { it.toCollection() }
            _allCollections.update { collections }
        }.onFailure { error ->
            safehillLogger.error("Failed to fetch collections. $error")
            throw error
        }
    }

    private suspend fun refreshTopPicks() {
        val topPicksResult = safeApiCall {
            serverProxy.remoteServer.topPickCollections()
        }

        topPicksResult.onSuccess { dtos ->
            _topPicks.update { dtos.map { it.toCollection() } }
        }.onFailure { error ->
            safehillLogger.error("Failed to fetch top picks. $error")
            throw error
        }
    }


    /**
     * Get a single collection by ID
     */
    suspend fun getCollection(id: String): Result<CollectionModel> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.retrieveCollection(id).toCollection()
            }
        }
    }

    /**
     * Track when a user accesses a collection
     */
    suspend fun trackCollectionAccess(id: String): Result<Unit> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.trackCollectionAccess(id)
            }
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

    /**
     * Create a new collection
     */
    suspend fun createCollection(
        name: String,
        description: String
    ): Result<CollectionModel> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.createCollection(
                    name = name,
                    description = description
                ).toCollection()
            }.also { result ->
                result.onSuccess { collection ->
                    // Add to all collections (owned/accessed will be derived automatically)
                    _allCollections.update { it + collection }
                }
            }
        }
    }

    /**
     * Update a collection
     */
    suspend fun updateCollection(
        id: String,
        name: String? = null,
        description: String? = null,
        pricing: Double? = null
    ): Result<CollectionModel> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.updateCollection(
                    id = id,
                    name = name,
                    description = description,
                    pricing = pricing
                ).toCollection()
            }.also { result ->
                result.onSuccess { updatedCollection ->
                    updateCollectionInCache(updatedCollection)
                }
            }
        }
    }

    /**
     * Archive a collection
     */
    suspend fun archiveCollection(id: String): Result<Unit> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.archiveCollection(id)
            }.also { result ->
                result.onSuccess {
                    removeCollectionFromCache(id)
                }
            }
        }
    }

    /**
     * Delete a collection
     */
    suspend fun deleteCollection(id: String): Result<Unit> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.deleteCollection(id)
            }.also { result ->
                result.onSuccess {
                    removeCollectionFromCache(id)
                }
            }
        }
    }

    /**
     * Add assets to a collection
     */
    suspend fun addAssetsToCollection(
        id: String,
        request: CollectionAssetAddRequestDTO
    ): Result<CollectionAssetAddResultDTO> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.addAssetsToCollection(id, request)
            }.also { result ->
                result.onSuccess {
                    // Refresh the collection to get updated asset count
                    refreshCollection(id)
                }
            }
        }
    }

    /**
     * Copy assets between collections
     */
    suspend fun copyAssets(
        request: CollectionAssetCopyRequestDTO
    ): Result<CollectionAssetCopyResultDTO> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.copyAssets(request)
            }.also { result ->
                result.onSuccess {
                    // Refresh both collections
                    refreshCollection(request.sourceCollectionId)
                    refreshCollection(request.targetCollectionId)
                }
            }
        }
    }

    /**
     * Change collection visibility
     */
    suspend fun changeCollectionVisibility(
        id: String,
        request: CollectionChangeVisibilityRequestDTO
    ): Result<CollectionChangeVisibilityResultDTO> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.changeCollectionVisibility(id, request)
            }.also { result ->
                result.onSuccess {
                    // Refresh the collection
                    refreshCollection(id)
                }
            }
        }
    }

    /**
     * Check if user has access to a collection
     */
    suspend fun checkCollectionAccess(
        collectionId: String
    ): Result<AccessCheckResultDTO> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.checkCollectionAccess(collectionId)
            }
        }
    }

    /**
     * Create a checkout session for purchasing collection access
     */
    suspend fun createCheckoutSession(
        collectionId: String,
        request: CreateCheckoutSessionRequestDTO
    ): Result<CheckoutSessionDTO> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.remoteServer.createCheckoutSession(collectionId, request)
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

    /**
     * Refresh a specific collection
     */
    private fun refreshCollection(id: String) {
        userScope.launch {
            val result = safeApiCall {
                serverProxy.remoteServer.retrieveCollection(id).toCollection()
            }
            result.onSuccess { updatedCollection ->
                updateCollectionInCache(updatedCollection)
            }
        }
    }

    /**
     * Update a collection in all caches
     */
    private fun updateCollectionInCache(collection: CollectionModel) {
        // Update in allCollections (owned/accessed will be derived automatically)
        _allCollections.update { collections ->
            collections.map { if (it.id == collection.id) collection else it }
        }

        // Update in topPicks if present
        _topPicks.update { collections ->
            collections.map { if (it.id == collection.id) collection else it }
        }
    }

    /**
     * Remove a collection from all caches
     */
    private fun removeCollectionFromCache(id: String) {
        // Remove from allCollections (owned/accessed will be derived automatically)
        _allCollections.update { it.filter { collection -> collection.id != id } }
        _topPicks.update { it.filter { collection -> collection.id != id } }
    }

    override suspend fun userLoggedIn(user: LocalUser) {
        _currentUserId.update { user.identifier }
        refreshCollections()
    }

    override fun userLoggedOut() {
        _currentUserId.update { null }
        _allCollections.update { emptyList() }
        _topPicks.update { emptyList() }
    }
}

val SafehillClient.collectionsRepository
    get() = this.repositories.collectionsRepository
