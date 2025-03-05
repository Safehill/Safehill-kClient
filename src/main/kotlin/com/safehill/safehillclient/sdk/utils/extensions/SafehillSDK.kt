package com.safehill.safehillclient.sdk.utils.extensions

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import com.safehill.safehillclient.sdk.SafehillSDK

val SafehillSDK.userProvider
    get() = sdkModule.userProvider

// Modules
val SafehillSDK.networkModule
    get() = sdkModule.networkModule

val SafehillSDK.assetModule
    get() = sdkModule.assetModule

val SafehillSDK.controllersModule
    get() = sdkModule.controllersModule

val SafehillSDK.backgroundTasksRegistry
    get() = sdkModule.backgroundTasksRegistry

// Background Tasks
val SafehillSDK.uploadOperation
    get() = this.backgroundTasksRegistry.uploadOperation


// Repositories
val SafehillSDK.userDiscoveryRepository
    get() = this.repositories.userDiscoveryRepository

val SafehillSDK.threadsRepository
    get() = this.repositories.threadsRepository

val SafehillSDK.userAuthorizationRepository
    get() = this.repositories.userAuthorizationRepository

// Dependencies

val SafehillSDK.remoteServerEnvironment: RemoteServerEnvironment
    get() = networkModule.remoteServerEnvironment

val SafehillSDK.serverProxy
    get() = networkModule.serverProxy

fun SafehillSDK.getGroupIdLink(groupId: GroupId): String {
    val (schema: String, host: String) = "https://" to this.remoteServerEnvironment.hostName
    return "$schema$host/sng/share/$groupId"
}
