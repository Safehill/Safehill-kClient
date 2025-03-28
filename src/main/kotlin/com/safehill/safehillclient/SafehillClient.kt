package com.safehill.safehillclient

import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.api.auth.AuthApi
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.safehillclient.data.user.api.UserStorage
import com.safehill.safehillclient.manager.ClientManager
import com.safehill.safehillclient.manager.dependencies.Repositories
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.model.SignInResponse
import com.safehill.safehillclient.model.auth.state.AuthState
import com.safehill.safehillclient.model.auth.state.AuthStateHolder
import com.safehill.safehillclient.model.user.UserQrData
import com.safehill.safehillclient.model.user.toLocalUser
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.utils.extensions.serverProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

typealias ClientScope = CoroutineScope

class SafehillClient(
    val clientModule: ClientModule,
    val repositories: Repositories,
    val clientManager: ClientManager,
    private val authStateHolder: AuthStateHolder,
    private val userStorage: UserStorage,
    private val authApi: AuthApi
) {

    private val loggedInUser = AtomicReference<LocalUser?>(null)

    private val userObserver = object : UserObserver {
        override suspend fun userLoggedIn(user: LocalUser) {
            // First set the user in the ClientModule. Then notify ClientManager of user existence.
            clientModule.userLoggedIn(user)
            clientManager.userLoggedIn(user)
        }

        override fun userLoggedOut() {
            // First notify ClientManager of logout. Then notify the ClientModule.
            clientManager.userLoggedOut()
            clientModule.userLoggedOut()
        }
    }

    suspend fun signIn(): Result<SignInResponse> {
        val storedUser = userStorage.getUser()
        return if (storedUser != null) {
            signIn(storedUser)
        } else {
            "User not found to initialize.".toFailure()
        }
    }

    suspend fun createUser(name: String): Result<SignInResponse> {
        return runCatching {
            require(userStorage.getUser() == null) {
                "Creating new user json will remove the stored user. Clear the user before proceeding."
            }
            val newUser = LocalUser(LocalCryptoUser()).also {
                it.name = name
            }
            authApi.createUser(
                name = name,
                identifier = newUser.identifier,
                publicKey = newUser.publicKey,
                signature = newUser.publicSignature
            )
            userStorage.storeUser(newUser)
            signIn(newUser).getOrThrow()
        }
    }

    private suspend fun signIn(user: LocalUser): Result<SignInResponse> {
        val loggedInUser = loggedInUser.get()
        return runCatchingSafe {
            if (loggedInUser != null) {
                if (loggedInUser.identifier == user.identifier) {
                    SignInResponse.SuccessWithUnknownMetadata(loggedInUser)
                } else {
                    throw UserContextMismatch()
                }
            } else {
                signInInternal(user).getOrThrow()
            }
        }
    }

    private suspend fun signInInternal(user: LocalUser): Result<SignInResponse.Success> {
        return runCatchingSafe {
            authStateHolder.setAuthState(AuthState.Loading)
            val response = authApi.signIn(user = user)
            user.authenticate(response.user, response)
            loggedInUser.set(user)
            userObserver.userLoggedIn(user)
            authStateHolder.setAuthState(AuthState.SignedOn(user = user))
            SignInResponse.Success(
                authResponseDTO = response,
                currentUser = user.updateFromServerUser(
                    response.user
                )
            )
        }.onFailure {
            authStateHolder.setAuthState(AuthState.SignedOff)
        }
    }

    suspend fun signInUserFromJson(userJson: String): Result<SignInResponse> {
        return runCatchingSafe {
            require(userStorage.getUser() == null) {
                "Signing in user from json will remove the stored user. Clear the user before proceeding."
            }
            try {
                val shLocalUser = Json.decodeFromString<UserQrData>(userJson).toLocalUser()
                val response = signIn(shLocalUser).getOrThrow()
                userStorage.storeUser(response.currentUser)
                response
            } catch (e: SerializationException) {
                throw "Invalid Data".toError()
            }
        }
    }

    private fun LocalUser.updateFromServerUser(serverUser: ServerUser) = apply {
        this.name = serverUser.name
    }

    fun logOut() {
        userObserver.userLoggedOut()
        loggedInUser.set(null)
        authStateHolder.setAuthState(AuthState.SignedOff)
    }

    suspend fun clear() {
        logOut()
        val storedUser = userStorage.getUser()
        if (storedUser != null) {
            clientManager.clearUserData(storedUser)
            userStorage.clear()
        }
    }

    fun getAuthState(): StateFlow<AuthState> {
        return authStateHolder.authState
    }

    suspend fun destroyAccount(): Result<Unit> {
        return runCatchingSafe {
            serverProxy.deleteAccount()
        }.onSuccess {
            clear()
        }
    }

    class UserContextMismatch : Exception(
        "User trying to sign in is different from already signed in user. Please sign out and sign in again."
    )
}

fun <T> String.toFailure(): Result<T> = Result.failure(this.toError())

fun String.toError() = Throwable(this)