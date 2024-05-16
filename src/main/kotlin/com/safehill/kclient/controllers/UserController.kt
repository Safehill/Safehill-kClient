package com.safehill.kclient.controllers

import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.ServerProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class UserController(
    private val serverProxy: ServerProxy
) {

    private val usersCache = ConcurrentHashMap<UserIdentifier, ServerUser>(50)

    suspend fun getUsers(userIdentifiers: List<UserIdentifier>): Result<Map<UserIdentifier, ServerUser>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val distinctIdentifiers = userIdentifiers.toSet().toMutableList()

                val userFromCache = distinctIdentifiers.mapNotNull { identifier ->
                    usersCache[identifier]
                }.associateBy { it.identifier }

                distinctIdentifiers.removeAll(userFromCache.keys)

                if (distinctIdentifiers.isEmpty()) {
                    userFromCache
                } else {
                    userFromCache + getUsersFromLocalOrServer(distinctIdentifiers)
                }
            }
        }
    }

    private suspend fun getUsersFromLocalOrServer(userIdentifiers: List<UserIdentifier>): Map<UserIdentifier, ServerUser> {
        val localUsers = serverProxy.localServer.getUsers(userIdentifiers)
        val remaining = userIdentifiers - localUsers.map { it.key }.toSet()

        val requiredUsers = if (remaining.isEmpty()) {
            localUsers
        } else {
            serverProxy.remoteServer.getUsers(remaining).also {
                serverProxy.localServer.upsertUsers(it.values.toList())
            } + localUsers
        }

        return requiredUsers
            .also { cacheUser(it.values) }
    }

    private fun cacheUser(users: Collection<ServerUser>) {
        users.forEach {
            usersCache.put(it.identifier, it)
        }
    }
}