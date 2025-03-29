package com.safehill.safehillclient.data.user.api

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.manager.dependencies.UserObserver
import java.util.Collections

interface UserObserverRegistry : UserObserver {
    fun addUserObserver(userObserver: UserObserver)
    fun removeUserObserver(userObserver: UserObserver)
}

class DefaultUserObserverRegistry(
    vararg userObserver: UserObserver
) : UserObserverRegistry {

    private val userObservers = Collections.synchronizedList(
        mutableListOf<UserObserver>(*userObserver)
    )

    override fun addUserObserver(userObserver: UserObserver) {
        synchronized(userObservers) {
            userObservers.add(userObserver)
        }
    }

    override fun removeUserObserver(userObserver: UserObserver) {
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