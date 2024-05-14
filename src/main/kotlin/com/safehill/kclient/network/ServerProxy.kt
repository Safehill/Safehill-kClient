package com.safehill.kclient.network

import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.local.LocalServerInterface

interface ServerProxy : SafehillApi {

    val localServer: LocalServerInterface
    val remoteServer: SafehillApi

    suspend fun getAllLocalUsers(): List<ServerUser>
}

