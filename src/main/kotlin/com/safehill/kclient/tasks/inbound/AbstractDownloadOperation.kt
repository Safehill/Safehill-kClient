package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.tasks.BackgroundTask
import com.safehill.kcrypto.models.ShareablePayload

abstract class AbstractDownloadOperation : DownloadOperation, BackgroundTask {

    private suspend fun getUsersInDescriptors(descriptors: List<AssetDescriptor>): Map<UserIdentifier, ServerUser> {
        val userIds = descriptors
            .map { d -> d.sharingInfo.sharedByUserIdentifier }
            .distinct()
        return this.getUsers(userIds)
    }

    override suspend fun run() {
        try {
            val descriptors = getDescriptors()
            val users = this.getUsersInDescriptors(descriptors)

            listeners.forEach { listener ->
                listener.received(
                    assetDescriptors = descriptors,
                    referencingUsers = users
                )
            }

            if (descriptors.isNotEmpty()) {
                process(descriptors)
            }
        } catch (e: Exception) {
            println("Error in download operation:$e " + e.message)
        }
    }

    override suspend fun processAssetsInDescriptors(descriptors: List<AssetDescriptor>) {
        val allUsersDict = this.getUsersInDescriptors(descriptors)
        val quality = AssetQuality.LowResolution
        val assetGIds = descriptors.map { it.globalIdentifier }.distinct()

        val assetsDict = this.getEncryptedAssets(
            withGlobalIdentifiers = assetGIds, versions = listOf(quality)
        )

        descriptors.forEach { descriptor ->
            val senderUser = allUsersDict[descriptor.sharingInfo.sharedByUserIdentifier]
            if (senderUser != null) {
                assetsDict[descriptor.globalIdentifier]?.let { encryptedAsset ->
                    val decryptedAsset = encryptedAsset.toDecryptedAsset(
                        senderUser = senderUser
                    )
                    listeners.forEach { it.fetched(decryptedAsset) }
                    RemoteDownloadOperation.alreadyProcessed.add(descriptor.globalIdentifier)
                }
            }
        }
    }

    private fun EncryptedAssetVersion.toShareablePayload() = ShareablePayload(
        ephemeralPublicKeyData = this.publicKeyData,
        ciphertext = this.encryptedSecret,
        signature = this.publicSignatureData,
        recipient = user
    )

    private fun EncryptedAsset.toDecryptedAsset(
        senderUser: ServerUser
    ): DecryptedAsset {
        val decryptedVersions = this.encryptedVersions.mapValues { (quality, encryptedVersion) ->
            val sharedSecret = encryptedVersion.toShareablePayload()
            user.decrypted(
                data = encryptedVersion.encryptedData,
                encryptedSecret = sharedSecret,
                receivedFrom = senderUser,
                protocolSalt = user.encryptionSalt
            )
        }
        return DecryptedAsset(
            globalIdentifier = this.globalIdentifier,
            localIdentifier = this.localIdentifier,
            creationDate = this.creationDate,
            decryptedVersions = decryptedVersions
        )
    }
}