package com.safehill.kclient.network

import com.safehill.kclient.api.AssetGlobalIdentifier
import com.safehill.kclient.api.SHSafehillAPI
import com.safehill.kclient.api.dtos.SHAuthResponse
import com.safehill.kclient.api.dtos.SHServerAsset
import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHAssetQuality
import com.safehill.kclient.models.SHEncryptedAsset
import com.safehill.kclient.models.SHRemoteUser
import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.user.SHLocalUserProtocol
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

class LocalServer(override var requestor: SHLocalUserProtocol): SHSafehillAPI {
    override suspend fun createUser(name: String): SHServerUser {
        TODO("Not yet implemented")
    }

    override suspend fun updateUser(name: String?): SHServerUser {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(name: String, password: String) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount() {
        TODO("Not yet implemented")
    }

    override suspend fun signIn(name: String): SHAuthResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getUsers(withIdentifiers: List<String>): List<SHRemoteUser> {
        TODO("Not yet implemented")
    }

    override suspend fun searchUsers(query: String): List<SHRemoteUser> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(): List<SHAssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(assetGlobalIdentifiers: List<AssetGlobalIdentifier>): List<SHAssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssets(
        globalIdentifiers: List<String>,
        versions: List<SHAssetQuality>?
    ): Map<String, SHEncryptedAsset> {
        TODO("Not yet implemented")
    }

    override suspend fun create(
        assets: List<SHEncryptedAsset>,
        groupId: String,
        filterVersions: List<SHAssetQuality>?
    ): List<SHServerAsset> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAssets(globalIdentifiers: List<String>): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        TODO("Not yet implemented")
    }

}
