package com.safehill.safehillclient

import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.auth.AuthApi
import com.safehill.safehillclient.manager.ClientManager
import com.safehill.safehillclient.model.SignInResponse
import com.safehill.safehillclient.model.auth.state.AuthState
import com.safehill.safehillclient.model.auth.state.AuthStateHolder
import com.safehill.safehillclient.module.client.ClientModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicReference

typealias ClientScope = CoroutineScope

class SafehillClient(
    val clientModule: ClientModule,
    private val authApi: AuthApi,
    private val clientManager: ClientManager
) {

    val authStateHolder = AuthStateHolder()

    val repositories = clientManager.repositories

    private val currentUserId = AtomicReference<String?>(null)

    suspend fun signIn(user: LocalUser): Result<SignInResponse> {
        return runCatching {
            val currentUser = authStateHolder.authenticatedUser.first()
            if (currentUser?.identifier == user.identifier) {
                SignInResponse.SuccessWithUnknownMetadata(currentUser)
            } else {
                authStateHolder.setAuthState(AuthState.Loading)
                val response = authApi.signIn(user = user)
                user.authenticate(response.user, response)
                setUserToSdk(user)
                authStateHolder.setAuthState(AuthState.SignedOn(user = user))
                SignInResponse.Success(authResponseDTO = response)
            }
        }.onFailure {
            logOut()
            authStateHolder.setAuthState(AuthState.SignedOff)
        }
    }

    suspend fun createUser(name: String): Result<LocalUser> {
        return runCatching {
            val newUser = LocalUser(LocalCryptoUser()).also {
                it.name = name
            }
            authApi.createUser(
                name = name,
                identifier = newUser.identifier,
                publicKey = newUser.publicKey,
                signature = newUser.publicSignature
            )
            newUser
        }
    }

    private fun logOut() {
        clientManager.userLoggedOut()
        clientModule.userLoggedOut()
        currentUserId.set(null)
        authStateHolder.setAuthState(AuthState.SignedOff)
    }

    suspend fun clear(user: LocalUser) {
        logOut()
        clientManager.clearUserData(user)
        authStateHolder.setAuthState(AuthState.NoUser)
    }

    private suspend fun setUserToSdk(user: LocalUser) {
        clientModule.userLoggedIn(user)
        clientManager.userLoggedIn(user)
        currentUserId.set(user.identifier)
    }
}