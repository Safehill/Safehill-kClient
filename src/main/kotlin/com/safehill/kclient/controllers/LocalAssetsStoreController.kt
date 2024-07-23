package com.safehill.kclient.controllers

import com.safehill.kclient.errors.BackgroundOperationError
import com.safehill.kclient.models.assets.Asset
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.LocalAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.network.ServerProxy
import com.safehill.kcrypto.models.SymmetricKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class LocalAssetsStoreController(
    private var serverProxy: ServerProxy,
    private var user: LocalUser,
) {
    suspend fun encryptedAsset(
        globalIdentifier: GlobalIdentifier,
        versions: List<AssetQuality>? = null,
        cacheHiResolution: Boolean,
    ): EncryptedAsset? {
        val encryptedAssets = encryptedAssets(
            globalIdentifiers = listOf(globalIdentifier),
            versions = versions,
            cacheHiResolution = cacheHiResolution,
        )
        return encryptedAssets[globalIdentifier]
    }

    suspend fun encryptedAssets(
        globalIdentifiers: List<GlobalIdentifier>,
        versions: List<AssetQuality>? = null,
        cacheHiResolution: Boolean,
    ): Map<GlobalIdentifier, EncryptedAsset> {
        return CoroutineScope(Dispatchers.IO).async {
            return@async serverProxy.getLocalAssets(
                globalIdentifiers = globalIdentifiers,
                versions = versions ?: AssetQuality.entries,
                cacheHiResolution = cacheHiResolution
            )
        }.await()
    }

    private suspend fun decryptedAssetInternal(
        encryptedAsset: EncryptedAsset,
        quality: AssetQuality,
        descriptor: AssetDescriptor,
    ): DecryptedAsset {
        val user = user
        if (descriptor.sharingInfo.sharedByUserIdentifier == user.identifier) {
            return user.decrypt(encryptedAsset, quality, user)
        } else {
            val usersDict = UserController(serverProxy).getUsers(
                listOf(descriptor.sharingInfo.sharedByUserIdentifier)
            ).getOrThrow()
            if (usersDict.size == 1) {
                val serverUser = usersDict.values.first()
                if (serverUser.identifier == descriptor.sharingInfo.sharedByUserIdentifier) {
                    return user.decrypt(encryptedAsset, quality, serverUser)
                } else {
                    throw BackgroundOperationError.UnexpectedData(usersDict)
                }
            } else {
                throw BackgroundOperationError.UnexpectedData(usersDict)
            }
        }
    }

    suspend fun decryptedAsset(
        encryptedAsset: EncryptedAsset,
        quality: AssetQuality,
        descriptor: AssetDescriptor? = null,
    ): DecryptedAsset {
        if (descriptor != null) {
            return decryptedAssetInternal(
                encryptedAsset = encryptedAsset,
                quality = quality,
                descriptor = descriptor,
            )
        } else {
            val descriptors = serverProxy.getLocalAssetDescriptors()
            val foundDescriptor = descriptors.firstOrNull { it.globalIdentifier == encryptedAsset.globalIdentifier }
            if (foundDescriptor != null) {
                return decryptedAssetInternal(
                    encryptedAsset = encryptedAsset,
                    quality = quality,
                    descriptor = foundDescriptor,
                )
            } else {
                throw BackgroundOperationError.MissingAssetInLocalServer(encryptedAsset.globalIdentifier)
            }
        }
    }

    suspend fun encryptionKey(globalIdentifier: AssetGlobalIdentifier): SymmetricKey? {
        return serverProxy.localServer.getEncryptionKey(globalIdentifier)
    }

    suspend fun saveEncryptionKey(globalIdentifier: AssetGlobalIdentifier, symmetricKey: SymmetricKey) {
        serverProxy.localServer.saveEncryptionKey(globalIdentifier, symmetricKey)
    }

}
