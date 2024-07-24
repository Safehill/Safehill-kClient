package com.safehill.kclient.controllers

import com.safehill.kclient.errors.DownloadError
import com.safehill.kclient.models.DownloadBlacklist
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.runCatchingPreservingCancellationException
import com.safehill.kcrypto.models.ShareablePayload

class AssetsDownloadManager(
    private val serverProxy: ServerProxy,
    private val userController: UserController
) {

    private val currentUser = serverProxy.requestor

    private val downloadBlackList = DownloadBlacklist()

    suspend fun downloadAsset(
        descriptor: AssetDescriptor
    ): Result<DecryptedAsset> {
        val isBlackListed = downloadBlackList.isBlacklisted(
            identifier = descriptor.globalIdentifier
        )
        return if (isBlackListed) {
            Result.failure(DownloadError.IsBlacklisted(identifier = descriptor.globalIdentifier))
        } else {
            val sharedByUserIdentifier = descriptor.sharingInfo.sharedByUserIdentifier
            userController.getUsers(
                userIdentifiers = listOf(sharedByUserIdentifier)
            ).mapCatching { usersMap ->
                downloadAsset(
                    descriptor = descriptor,
                    usersMap = usersMap
                )
            }
        }
    }

    private suspend fun downloadAsset(
        descriptor: AssetDescriptor,
        usersMap: Map<UserIdentifier, ServerUser>
    ): DecryptedAsset {
        val sharedByUserIdentifier = descriptor.sharingInfo.sharedByUserIdentifier
        val sharedByUser = usersMap[sharedByUserIdentifier]
        return if (sharedByUser == null) {
            throw DownloadError.SharedByUserNotFound(assetDescriptor = descriptor)
        } else {
            val encryptedAsset = downloadAsset(
                assetDescriptor = descriptor,
                quality = AssetQuality.LowResolution
            )
            encryptedAsset.toDecryptedAsset(
                senderUser = sharedByUser
            )
        }
    }

    private fun EncryptedAssetVersion.toShareablePayload() = ShareablePayload(
        ephemeralPublicKeyData = this.publicKeyData,
        ciphertext = this.encryptedSecret,
        signature = this.publicSignatureData,
        recipient = currentUser
    )

    private fun EncryptedAsset.toDecryptedAsset(
        senderUser: ServerUser
    ): DecryptedAsset {
        val decryptedVersions = this.encryptedVersions.mapValues { (_, encryptedVersion) ->
            val sharedSecret = encryptedVersion.toShareablePayload()
            currentUser.decrypted(
                data = encryptedVersion.encryptedData,
                encryptedSecret = sharedSecret,
                receivedFrom = senderUser,
                protocolSalt = currentUser.encryptionSalt
            )
        }
        return DecryptedAsset(
            globalIdentifier = this.globalIdentifier,
            localIdentifier = this.localIdentifier,
            creationDate = this.creationDate,
            decryptedVersions = decryptedVersions
        )
    }

    private suspend fun downloadAsset(
        assetDescriptor: AssetDescriptor,
        quality: AssetQuality
    ): EncryptedAsset {
        val identifier = assetDescriptor.globalIdentifier
        return runCatchingPreservingCancellationException {
            val assets = serverProxy.getAssets(
                globalIdentifiers = listOf(identifier),
                versions = listOf(quality)
            )
            assets[identifier] ?: throw DownloadError.AssetNotFound(
                assetDescriptor = assetDescriptor
            )
        }.onFailure {
            downloadBlackList.recordFailedAttempt(
                identifier = identifier
            )
        }.getOrThrow()
    }
}