package com.safehill.kclient.network

import com.safehill.kclient.models.users.ServerUser

interface ServerProxyInterface : SafehillApi {
    suspend fun getAllLocalUsers(): List<ServerUser>
}

