package com.safehill.kclient.controllers

import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.user.SHLocalUserInterface
import com.safehill.kclient.network.ServerProxyInterface

class UsersController(
    private val localUser: SHLocalUserInterface,
    private var serverProxy: ServerProxyInterface = localUser.serverProxy
) {

    @Throws(Exception::class)
    suspend fun getUsers(userIdentifiers: List<String>): List<SHServerUser> {
        val users = mutableListOf<SHServerUser>()
        val foundUserIds = mutableListOf<String>()
        val missingUserIds = mutableListOf<String>()

        for (userIdentifier in userIdentifiers) {
            if (!foundUserIds.contains(userIdentifier)) {
                val user: SHServerUser? = null //TODO: ServerUserCache.shared.userWith(userIdentifier)
                if (user != null) {
                    users.add(user)
                    foundUserIds.add(userIdentifier)
                } else if (!missingUserIds.contains(userIdentifier)) {
                    missingUserIds.add(userIdentifier)
                }
            }
        }

        if (missingUserIds.isEmpty()) {
            return users
        }

        val result = serverProxy.getUsers(missingUserIds)
        users.addAll(result)

        //TODO: ServerUserCache.shared.cache(users)

        return users
    }

    @Throws(Exception::class)
    suspend fun getAllLocalUsers(): List<SHServerUser> {
        return serverProxy.getAllLocalUsers()
    }

}