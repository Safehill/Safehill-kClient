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
import com.safehill.safehillclient.phone_number_verification.PhoneNumberVerificationStatusBroadcasterImpl
import kotlinx.coroutines.flow.StateFlow

class AuthenticationCoordinator(
    private val userStorage: UserStorage,
    private val userValidator: UserValidator,
    private val sessionManager: SessionManager,
    private val authStateManager: AuthStateManager,
    private val onlineAuthStrategy: OnlineAuthenticationStrategy,
    private val offlineAuthStrategy: OfflineAuthenticationStrategy
) {

    internal val phoneNumberVerificationStatusBroadcaster =
        PhoneNumberVerificationStatusBroadcasterImpl()

    suspend fun signIn(): Result<SignInResponse> {
        return runCatching {
            sessionManager.runLoginSession {
                val storedUser = validateAndGetStoredUser()
                performSignIn(storedUser)
            }
        }
    }

    private suspend fun validateAndGetStoredUser(): LocalUser {
        val storedUser = userStorage.getUser() ?: throw LocalUserError.UnAvailable()
        userValidator.validateUserData(storedUser)
        return storedUser
    }

    suspend fun performSignIn(user: LocalUser): SignInResponse {
        try {
            authStateManager.setLoading()
            val existingSession = sessionManager.getExistingSessionForUser(user)
            return existingSession ?: run {
                val signInResponse = attemptOnlineSignIn(user).getOrThrow()
                phoneNumberVerificationStatusBroadcaster.broadCastVerificationStatus(
                    signInResponse.isPhoneNumberVerified
                )
                val updatedUser = signInResponse.currentUser
                userStorage.storeUser(updatedUser)
                signInResponse
            }
        } catch (error: Throwable) {
            authStateManager.setSignedOff()
            throw error
        }
    }

    private suspend fun attemptOnlineSignIn(user: LocalUser): Result<SignInResponse> {
        return runCatchingSafe {
            val response = onlineAuthStrategy.authenticate(user)
            completeSignIn(response.currentUser)
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
        try {
            sessionManager.setLoggedInUser(user)
            authStateManager.setSignedOn(user)
        } catch (e: Exception) {
            // Swallow cleaning up errors
            runCatchingSafe { logOut() }
            throw e
        }
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