package com.safehill.safehillclient.sdk.utils.extensions

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import com.safehill.safehillclient.sdk.SafehillClient

val SafehillClient.userProvider
    get() = clientModule.userProvider

// Modules
val SafehillClient.networkModule
    get() = clientModule.networkModule

val SafehillClient.assetModule
    get() = clientModule.assetModule

val SafehillClient.controllersModule
    get() = clientModule.controllersModule

val SafehillClient.backgroundTasksRegistry
    get() = clientModule.backgroundTasksRegistry

// Background Tasks
val SafehillClient.uploadOperation
    get() = this.backgroundTasksRegistry.uploadOperation


// Repositories
val SafehillClient.userDiscoveryRepository
    get() = this.repositories.userDiscoveryRepository

val SafehillClient.threadsRepository
    get() = this.repositories.threadsRepository

val SafehillClient.userAuthorizationRepository
    get() = this.repositories.userAuthorizationRepository

// Dependencies

val SafehillClient.remoteServerEnvironment: RemoteServerEnvironment
    get() = networkModule.remoteServerEnvironment

val SafehillClient.serverProxy
    get() = networkModule.serverProxy

fun SafehillClient.getGroupIdLink(groupId: GroupId): String {
    val (schema: String, host: String) = "https://" to this.remoteServerEnvironment.hostName
    return "$schema$host/sng/share/$groupId"
}
