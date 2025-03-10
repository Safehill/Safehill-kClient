package com.safehill.safehillclient.data.user.api

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.manager.dependencies.UserObserver
import java.util.Collections

class UserObserverRegistry : UserObserver {
    private val userObservers = Collections.synchronizedList(mutableListOf<UserObserver>())

    fun addUserObserver(userObserver: UserObserver) {
        synchronized(userObservers) {
            userObservers.add(userObserver)
        }
    }

    fun removeUserObserver(userObserver: UserObserver) {
        synchronized(userObservers) {
            userObservers.remove(userObserver)
        }
    }

    override suspend fun userLoggedIn(user: LocalUser) {
        userObservers.forEach { it.userLoggedIn(user) }
    }

    override fun userLoggedOut() {
        userObservers.forEach { it.userLoggedOut() }
    }
}