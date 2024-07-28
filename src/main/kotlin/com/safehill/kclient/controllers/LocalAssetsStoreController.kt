package com.safehill.kclient.controllers

import com.safehill.SafehillClient
import com.safehill.kclient.errors.CipherError
import com.safehill.kclient.errors.DownloadError
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.toDecryptedAsset
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.util.runCatchingPreservingCancellationException
import com.safehill.kcrypto.models.SymmetricKey

class LocalAssetsStoreController(
    safehillClient: SafehillClient
) {
    private val serverProxy = safehillClient.serverProxy
    private val currentUser = safehillClient.currentUser
    private val userController = safehillClient.userController

    suspend fun getAsset(
        globalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
        descriptor: AssetDescriptor? = null
    ): Result<DecryptedAsset> {
        return runCatchingPreservingCancellationException {
            val assetDescriptor = descriptor ?: run {
                val descriptors = serverProxy.getAssetDescriptors(
                    assetGlobalIdentifiers = listOf(globalIdentifier),
                    groupIds = null, after = null
                )
                descriptors.firstOrNull { it.globalIdentifier == globalIdentifier }
                    ?: throw DownloadError.AssetDescriptorNotFound(globalIdentifier)
            }
            val senderUser = getSenderUser(
                descriptor = assetDescriptor
            )
            val encryptedAsset = downloadAsset(
                assetDescriptor = assetDescriptor,
                quality = quality
            )
            encryptedAsset.toDecryptedAsset(
                senderUser = senderUser,
                currentUser = currentUser
            )
        }

    }

    private suspend fun downloadAsset(
        assetDescriptor: AssetDescriptor,
        quality: AssetQuality
    ): EncryptedAsset {
        val identifier = assetDescriptor.globalIdentifier
        return serverProxy.getAsset(
            globalIdentifier = identifier,
            quality = quality,
            cacheAfterFetch = false
        )
    }

    private suspend fun getSenderUser(
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


    suspend fun encryptionKey(globalIdentifier: AssetGlobalIdentifier): SymmetricKey? {
        return serverProxy.localServer.getEncryptionKey(globalIdentifier)
    }

    suspend fun saveEncryptionKey(
        globalIdentifier: AssetGlobalIdentifier,
        symmetricKey: SymmetricKey
    ) {
        serverProxy.localServer.saveEncryptionKey(globalIdentifier, symmetricKey)
    }

}
