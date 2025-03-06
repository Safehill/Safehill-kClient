package com.safehill.safehillclient.sdk

import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.auth.AuthApi
import com.safehill.safehillclient.sdk.error.UserContextMismatch
import com.safehill.safehillclient.sdk.model.SignInResponse
import com.safehill.safehillclient.sdk.module.sdk.ClientManager
import com.safehill.safehillclient.sdk.module.sdk.SdkModule
import com.safehill.safehillclient.sdk.state.AuthState
import com.safehill.safehillclient.sdk.state.AuthStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicReference

typealias SDKScope = CoroutineScope

class SafehillSDK(
    val sdkModule: SdkModule,
    private val authApi: AuthApi
) {

    val authStateHolder = AuthStateHolder()

    private val currentUserId = AtomicReference<String?>(null)

    private val clientManager = ClientManager(sdkModule)

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
        sdkModule.clearUser(clearPersistence = clearPersistence)
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