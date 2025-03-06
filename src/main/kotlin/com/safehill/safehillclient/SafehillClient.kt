package com.safehill.safehillclient

import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.auth.AuthApi
import com.safehill.safehillclient.error.UserContextMismatch
import com.safehill.safehillclient.manager.ClientManager
import com.safehill.safehillclient.model.SignInResponse
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.model.auth.state.AuthState
import com.safehill.safehillclient.model.auth.state.AuthStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicReference

typealias ClientScope = CoroutineScope

class SafehillClient(
    val clientModule: ClientModule,
    private val authApi: AuthApi
) {

    val authStateHolder = AuthStateHolder()

    private val currentUserId = AtomicReference<String?>(null)

    private val clientManager = ClientManager(clientModule)

    val repositories = clientManager.repositories

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
            signOut(clearPersistence = false)
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

    suspend fun signOut(clearPersistence: Boolean) {
        clientModule.clearUser(clearPersistence = clearPersistence)
        authStateHolder.setAuthState(AuthState.SignedOff)
    }

    suspend fun clear(user: LocalUser) {
        val currentUserId = currentUserId.get()
        if (currentUserId == null) {
            setUserToSdk(user)
        } else if (currentUserId != user.identifier) {
            throw UserContextMismatch()
        }
        signOut(clearPersistence = true)
    }

    private suspend fun setUserToSdk(user: LocalUser) {
        clientManager.userSet(user)
        currentUserId.set(user.identifier)
    }
}