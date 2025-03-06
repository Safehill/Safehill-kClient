package com.safehill.safehillclient.data.authorization

import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.dtos.websockets.NewConnectionRequest
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.tasks.syncing.InteractionSync
import com.safehill.kclient.util.safeApiCall
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.data.user.model.AppUser
import com.safehill.safehillclient.data.user.model.toAppUser
import com.safehill.safehillclient.module.client.UserScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserAuthorizationRepository(
    private val interactionSync: InteractionSync,
    private val serverProxy: ServerProxy,
    private val userScope: UserScope,
    private val webSocketApi: WebSocketApi,
    private val safehillLogger: SafehillLogger
) : UserObserver {


    private val _unAuthorizedUsers = MutableStateFlow(listOf<AppUser>())
    val unAuthorizedUsers = _unAuthorizedUsers.asStateFlow()

    private val _blockedUsers = MutableStateFlow(listOf<AppUser>())
    val blockedUsers = _blockedUsers.asStateFlow()

    private fun listenForNewAuthorizationEvents() {
        userScope.launch {
            webSocketApi.socketMessages.collect { socketMessage ->
                if (socketMessage is NewConnectionRequest) {
                    _unAuthorizedUsers.update { initial -> initial + socketMessage.requestor.toAppUser() }
                }
            }
        }
    }

    private fun syncInteractions() {
        userScope.launch {
            interactionSync.run()
        }
    }

    fun getUnAuthorizedAndBlockedUsers() {
        userScope.launch {
            safeApiCall { serverProxy.getAuthorizationStatus() }
                .onSuccess { authorizationStatusDTO ->
                    _unAuthorizedUsers.update { authorizationStatusDTO.pending.map(ServerUser::toAppUser) }
                    _blockedUsers.update { authorizationStatusDTO.blocked.map(ServerUser::toAppUser) }
                }.onFailure {
                    safehillLogger.error("Failed to get authorization status $it")
                }
        }
    }

    suspend fun authorizeUser(user: AppUser): Result<Unit> {
        return safeApiCall {
            serverProxy.authorizeUsers(listOf(user.identifier))
        }.onSuccess {
            _unAuthorizedUsers.update { initial -> initial - user }
            _blockedUsers.update { initial -> initial - user }
            refreshAuthorizationStatusAndInteractions()
        }.onFailure {
            safehillLogger.error("Failed to authorize user $it")
        }
    }

    private fun refreshAuthorizationStatusAndInteractions() {
        getUnAuthorizedAndBlockedUsers()
        syncInteractions()
    }

    suspend fun blockUser(user: AppUser): Result<Unit> {
        return safeApiCall {
            serverProxy.blockUsers(listOf(user.identifier))
        }.onSuccess {
            _unAuthorizedUsers.update { initial -> initial - user }
            _blockedUsers.update { initial -> initial + user }
            refreshAuthorizationStatusAndInteractions()
        }.onFailure {
            safehillLogger.error("Failed to block user $it")
        }
    }

    override suspend fun userSet(user: LocalUser) {
        getUnAuthorizedAndBlockedUsers()
        listenForNewAuthorizationEvents()
    }

    override fun clearUser(clearPersistence: Boolean) {
        _unAuthorizedUsers.update { emptyList() }
        _blockedUsers.update { emptyList() }
    }

}