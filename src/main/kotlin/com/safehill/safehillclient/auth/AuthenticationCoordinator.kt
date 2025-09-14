package com.safehill.safehillclient.auth

import com.safehill.kclient.errors.LocalUserError
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.auth.AuthApi
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.safehillclient.data.user.api.UserStorage
import com.safehill.safehillclient.manager.ClientManager
import com.safehill.safehillclient.model.SignInResponse
import com.safehill.safehillclient.model.auth.state.AuthState
import com.safehill.safehillclient.module.client.ClientModule
import kotlinx.coroutines.flow.StateFlow

class AuthenticationCoordinator(
    private val userStorage: UserStorage,
    private val userValidator: UserValidator,
    private val sessionManager: SessionManager,
    private val authStateManager: AuthStateManager,
    private val onlineAuthStrategy: OnlineAuthenticationStrategy,
    private val offlineAuthStrategy: OfflineAuthenticationStrategy
) {

    suspend fun signIn(): Result<SignInResponse> {
        return runCatching {
            try {
                val started = sessionManager.startSignInProcess()
                if (!started) {
                    throw IllegalStateException("Sign in already in progress.")
                }
                val storedUser = validateAndGetStoredUser()
                val existingSession = sessionManager.getExistingSessionForUser(storedUser)
                existingSession ?: performSignIn(storedUser)
            } finally {
                sessionManager.endSignInProcess()
            }
        }
    }

    private suspend fun validateAndGetStoredUser(): LocalUser {
        val storedUser = userStorage.getUser() ?: throw LocalUserError.UnAvailable()
        userValidator.validateUserData(storedUser)
        return storedUser
    }

    private suspend fun performSignIn(user: LocalUser): SignInResponse {
        try {
            authStateManager.setLoading()
            return attemptOnlineSignIn(user).getOrElse { error ->
                attemptOfflineSignIn(user, error).getOrThrow()
            }
        } catch (error: Throwable) {
            authStateManager.setSignedOff()
            throw error
        }
    }

    private suspend fun attemptOnlineSignIn(user: LocalUser): Result<SignInResponse> {
        return runCatchingSafe {
            val response = onlineAuthStrategy.authenticate(user)
            completeSignIn(user)
            response
        }
    }

    private suspend fun attemptOfflineSignIn(
        user: LocalUser,
        originalError: Throwable
    ): Result<SignInResponse> {
        return runCatchingSafe {
            val response = offlineAuthStrategy.authenticate(user)
            completeSignIn(user)
            response
        }
    }

    private suspend fun completeSignIn(
        user: LocalUser
    ) {
        sessionManager.setLoggedInUser(user)
        authStateManager.setSignedOn(user)
    }

    fun getAuthState(): StateFlow<AuthState> {
        return authStateManager.getAuthState()
    }

    fun logOut() {
        sessionManager.logOut()
        authStateManager.setSignedOff()
    }

    class Factory {
        fun create(
            userStorage: UserStorage,
            clientModule: ClientModule,
            clientManager: ClientManager,
            authApi: AuthApi
        ): AuthenticationCoordinator {
            val userValidator = UserValidator()
            return AuthenticationCoordinator(
                userStorage = userStorage,
                userValidator = userValidator,
                sessionManager = SessionManager(
                    clientModule = clientModule,
                    userObserverRegistry = clientManager
                ),
                authStateManager = AuthStateManager(),
                onlineAuthStrategy = OnlineAuthenticationStrategy(
                    userValidator = userValidator,
                    authApi = authApi
                ),
                offlineAuthStrategy = OfflineAuthenticationStrategy(
                    userValidator = userValidator
                )
            )
        }
    }
}