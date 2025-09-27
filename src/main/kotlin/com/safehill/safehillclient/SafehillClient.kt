package com.safehill.safehillclient

import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.auth.AuthApi
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.safehillclient.auth.AuthenticationCoordinator
import com.safehill.safehillclient.data.user.api.UserStorage
import com.safehill.safehillclient.manager.ClientManager
import com.safehill.safehillclient.manager.dependencies.Repositories
import com.safehill.safehillclient.model.SignInResponse
import com.safehill.safehillclient.model.auth.state.AuthState
import com.safehill.safehillclient.model.user.UserQrData
import com.safehill.safehillclient.model.user.toLocalUser
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.utils.extensions.serverProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

typealias ClientScope = CoroutineScope

class SafehillClient(
    val clientModule: ClientModule,
    val repositories: Repositories,
    val clientManager: ClientManager,
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val userStorage: UserStorage,
    private val authApi: AuthApi
) {

    suspend fun signIn(): Result<SignInResponse> {
        return authenticationCoordinator.signIn()
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
            authenticationCoordinator.signIn().getOrThrow()
        }
    }

    suspend fun signInUserFromJson(userJson: String): Result<SignInResponse> {
        return runCatchingSafe {
            require(userStorage.getUser() == null) {
                "Signing in user from json will remove the stored user. Clear the user before proceeding."
            }
            try {
                val shLocalUser = Json.decodeFromString<UserQrData>(userJson).toLocalUser()
                authenticationCoordinator.performSignIn(shLocalUser)
            } catch (e: SerializationException) {
                throw "Invalid Data".toError()
            }
        }
    }

    fun logOut(): Result<Unit> {
        return runCatching {
            authenticationCoordinator.logOut()
        }
    }

    suspend fun clear(): Result<Unit> {
        return runCatching {
            logOut().getOrThrow()
            val storedUser = userStorage.getUser()
            if (storedUser != null) {
                clientManager.clearUserData(storedUser)
                userStorage.clear()
            }
        }
    }

    fun getAuthState(): StateFlow<AuthState> {
        return authenticationCoordinator.getAuthState()
    }

    suspend fun destroyAccount(): Result<Unit> {
        return runCatchingSafe {
            serverProxy.deleteAccount()
        }.onSuccess {
            clear()
        }
    }


}

fun <T> String.toFailure(): Result<T> = Result.failure(this.toError())

fun String.toError() = Throwable(this)