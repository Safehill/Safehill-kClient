package com.safehill.kclient.controllers

import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.network.ServerProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class UserController(
    private val serverProxy: ServerProxy
) {

    private val usersCache = ConcurrentHashMap<String, SHServerUser>(50)

    suspend fun getUsers(userIdentifiers: List<String>): Result<Map<String, SHServerUser>> {
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

    private suspend fun getUsersFromLocalOrServer(userIdentifiers: List<String>): Map<String, SHServerUser> {
        val localUsers = serverProxy.localServer.getUsers(userIdentifiers)
        val remaining = userIdentifiers - localUsers.map { it.identifier }.toSet()

        val requiredUsers = if (remaining.isEmpty()) {
            localUsers
        } else {
            serverProxy.remoteServer.getUsers(remaining).also {
                serverProxy.localServer.upsertUsers(it)
            } + localUsers
        }

        return requiredUsers
            .also(::cacheUser)
            .associateBy { it.identifier }
    }

    private fun cacheUser(users: List<SHServerUser>) {
        users.forEach {
            usersCache.put(it.identifier, it)
        }
    }
}