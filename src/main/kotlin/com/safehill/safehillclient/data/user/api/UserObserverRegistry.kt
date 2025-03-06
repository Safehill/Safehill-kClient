package com.safehill.safehillclient.data.user.api

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.manager.dependencies.UserObserver
import java.util.Collections

class UserObserverRegistry : UserObserver {
    private val userObservers = Collections.synchronizedList(mutableListOf<UserObserver>())

    fun addUserObserver(userObserver: UserObserver) {
        userObservers.add(userObserver)
    }

    fun removeUserObserver(userObserver: UserObserver) {
        userObservers.remove(userObserver)
    }

    override suspend fun userSet(user: LocalUser) {
        userObservers.forEach { it.userSet(user) }
    }

    override fun clearUser(clearPersistence: Boolean) {
        userObservers.forEach { it.clearUser(clearPersistence) }
    }
}