package com.safehill.kclient.controllers

import com.safehill.kclient.errors.CipherError
import com.safehill.kclient.errors.DownloadError
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.toDecryptedAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.runCatchingSafe

class LocalAssetsStoreController(
    private val serverProxy: ServerProxy,
    private val userController: UserController,
    private val assetDescriptorsCache: AssetDescriptorsCache,
    private val userProvider: UserProvider
) {


    suspend fun getAsset(
        globalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
        descriptor: AssetDescriptor? = null,
        cacheAfterFetch: Boolean
    ): Result<DecryptedAsset> {
        return runCatchingSafe {
            val assetDescriptor =
                descriptor ?: assetDescriptorsCache.getDescriptor(globalIdentifier) ?: run {
                    val descriptors = serverProxy.getAssetDescriptors(
                        assetGlobalIdentifiers = listOf(globalIdentifier),
                        groupIds = null, after = null
                    )
                    descriptors
                        .firstOrNull { it.globalIdentifier == globalIdentifier }
                        ?.also(assetDescriptorsCache::upsertAssetDescriptor)
                        ?: throw DownloadError.AssetDescriptorNotFound(globalIdentifier)
                }
            val currentUser = userProvider.get()
            val senderUser = getSenderUser(
                currentUser = currentUser,
                descriptor = assetDescriptor
            )
            val encryptedAsset = downloadAsset(
                assetDescriptor = assetDescriptor,
                quality = quality,
                cacheAfterFetch = cacheAfterFetch
            )
            encryptedAsset.toDecryptedAsset(
                senderUser = senderUser,
                currentUser = currentUser
            )
        }
    }

    private suspend fun downloadAsset(
        assetDescriptor: AssetDescriptor,
        quality: AssetQuality,
        cacheAfterFetch: Boolean
    ): EncryptedAsset {
        val identifier = assetDescriptor.globalIdentifier
        return serverProxy.getAsset(
            globalIdentifier = identifier,
            qualities = listOf(quality),
            cacheAfterFetch = cacheAfterFetch
        )
    }

    private suspend fun getSenderUser(
        currentUser: LocalUser,
        descriptor: AssetDescriptor,
    ): ServerUser {
        val senderUserIdentifier = descriptor.sharingInfo.sharedByUserIdentifier
        return if (senderUserIdentifier == currentUser.identifier) {
            currentUser
        } else {
            val usersDict = userController.getUsers(
                listOf(senderUserIdentifier)
            ).getOrThrow()
            usersDict[senderUserIdentifier] ?: throw CipherError.UnexpectedData(usersDict)
        }
    }

}
