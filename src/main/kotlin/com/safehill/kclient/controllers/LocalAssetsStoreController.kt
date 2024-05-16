package com.safehill.kclient.controllers

import com.safehill.kclient.GlobalIdentifier
import com.safehill.kclient.errors.SHBackgroundOperationError
import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHAssetQuality
import com.safehill.kclient.models.SHDecryptedAsset
import com.safehill.kclient.models.SHEncryptedAsset
import com.safehill.kclient.models.SHLocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.ServerProxyInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class LocalAssetsStoreController(
    private var serverProxy: ServerProxyInterface,
    private var user: SHLocalUser,
) {
    suspend fun encryptedAsset(
        globalIdentifier: GlobalIdentifier,
        versions: List<SHAssetQuality>? = null,
        cacheHiResolution: Boolean,
    ): SHEncryptedAsset? {
        val encryptedAssets = encryptedAssets(
            globalIdentifiers = listOf(globalIdentifier),
            versions = versions,
            cacheHiResolution = cacheHiResolution,
        )
        return encryptedAssets[globalIdentifier]
    }

    suspend fun encryptedAssets(
        globalIdentifiers: List<GlobalIdentifier>,
        versions: List<SHAssetQuality>? = null,
        cacheHiResolution: Boolean,
    ): Map<GlobalIdentifier, SHEncryptedAsset> {
        return CoroutineScope(Dispatchers.IO).async {
            return@async serverProxy.getLocalAssets(
                globalIdentifiers = globalIdentifiers,
                versions = versions ?: SHAssetQuality.entries,
                cacheHiResolution = cacheHiResolution
            )
        }.await()
    }

    private suspend fun decryptedAssetInternal(
        encryptedAsset: SHEncryptedAsset,
        quality: SHAssetQuality,
        descriptor: SHAssetDescriptor,
    ): SHDecryptedAsset {
        val user = user
        if (descriptor.sharingInfo.sharedByUserIdentifier == user.identifier) {
            return user.decrypt(encryptedAsset, quality, user)
        } else {
            val usersDict = UserController(serverProxy as ServerProxy).getUsers(
                listOf(descriptor.sharingInfo.sharedByUserIdentifier)
            ).getOrThrow()
            if (usersDict.size == 1) {
                val serverUser = usersDict.values.first()
                if (serverUser.identifier == descriptor.sharingInfo.sharedByUserIdentifier) {
                    return user.decrypt(encryptedAsset, quality, serverUser)
                } else {
                    throw SHBackgroundOperationError.UnexpectedData(usersDict)
                }
            } else {
                throw SHBackgroundOperationError.UnexpectedData(usersDict)
            }
        }
    }

    suspend fun decryptedAsset(
        encryptedAsset: SHEncryptedAsset,
        quality: SHAssetQuality,
        descriptor: SHAssetDescriptor? = null,
    ): SHDecryptedAsset {
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
                throw SHBackgroundOperationError.MissingAssetInLocalServer(encryptedAsset.globalIdentifier)
            }
        }
    }

}