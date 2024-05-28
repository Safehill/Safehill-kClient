package com.safehill.kclient.network

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.local.LocalServerInterface

typealias GlobalIdentifier = String
interface ServerProxy : SafehillApi {

    val localServer: LocalServerInterface
    val remoteServer: SafehillApi

    suspend fun getAllLocalUsers(): List<ServerUser>
    @Throws(Exception::class)
    suspend fun getLocalAssets(globalIdentifiers: List<GlobalIdentifier>, versions: List<AssetQuality>, cacheHiResolution: Boolean): Map<String, EncryptedAsset>
    @Throws(Exception::class)
    suspend fun getLocalAssetDescriptors(globalIdentifiers: List<GlobalIdentifier>? = null, filteringGroups: List<String>? = null): List<AssetDescriptor>
//    suspend fun getUsers(withIdentifiers: List<AssetDescriptor>): Map<UserIdentifier, RemoteUser>
}

