package com.safehill.safehillclient.auth

import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.getOrNull
import com.safehill.safehillclient.data.user.api.UserObserverRegistry
import com.safehill.safehillclient.model.SignInResponse
import com.safehill.safehillclient.module.client.ClientModule
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionManager(
    private val clientModule: ClientModule,
    private val userObserverRegistry: UserObserverRegistry
) {

    private val mutex = Mutex()
    suspend fun <T> runLoginSession(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }


    fun getExistingSessionForUser(requestedUser: LocalUser): SignInResponse? {
        val currentUser = clientModule.userProvider.getOrNull() ?: return null

        return if (currentUser.identifier == requestedUser.identifier) {
            SignInResponse.SuccessWithUnknownMetadata(currentUser)
        } else {
            throw UserContextMismatch()
        }
    }

    suspend fun setLoggedInUser(user: LocalUser) {
        // Set user first
        clientModule.userLoggedIn(user)

        // Notify the listeners then
        userObserverRegistry.userLoggedIn(user)
    }


    fun logOut() {

        // Notify the listeners first.
        userObserverRegistry.userLoggedOut()
        // Clear the user
        clientModule.userLoggedOut()

        val currentUser = clientModule.userProvider.getOrNull()
        currentUser?.deauthenticate()
    }

    class UserContextMismatch : Exception(
        "User trying to sign in is different from already signed in user. Please sign out and sign in again."
    )
}