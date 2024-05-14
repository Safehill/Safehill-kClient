package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAssetImpl
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.tasks.BackgroundTask
import com.safehill.kcrypto.models.ShareablePayload

abstract class AbstractDownloadOperation: DownloadOperation, BackgroundTask {

    suspend fun getUsersInDescriptors(descriptors: List<AssetDescriptor>): Map<UserIdentifier, ServerUser> {
        val userIds = descriptors
            .map { d -> d.sharingInfo.sharedByUserIdentifier }
            .distinct()
        return this.getUsers(userIds)
    }

    override suspend fun run() {
        val descriptors = getDescriptors()
        val users = this.getUsersInDescriptors(descriptors)

        listeners.forEach { listener -> listener.received(descriptors, referencingUsers = users) }

        if (descriptors.isNotEmpty()) {
            process(descriptors)
        }
    }

    override suspend fun processAssetsInDescriptors(descriptors: List<AssetDescriptor>) {
        val allUsersDict = this.getUsersInDescriptors(descriptors)
        val quality = AssetQuality.LowResolution
        val assetGIds = descriptors.map { it.globalIdentifier }.distinct()
        val assetsDict = this.getEncryptedAssets(assetGIds, versions = listOf(quality))

        descriptors.forEach { descriptor ->
            allUsersDict[descriptor.sharingInfo.sharedByUserIdentifier]?.let { senderUser ->
                assetsDict[descriptor.globalIdentifier]?.let { encryptedAsset ->
                    encryptedAsset.encryptedVersions.get(quality)?.let { assetVersion ->
                        val sharedSecret = ShareablePayload(
                            ephemeralPublicKeyData = assetVersion.publicKeyData,
                            ciphertext = assetVersion.encryptedSecret,
                            signature = assetVersion.publicSignatureData,
                            recipient = this.user
                        )
                        val decryptedData = this.user.decrypted(
                            assetVersion.encryptedData,
                            encryptedSecret = sharedSecret,
                            protocolSalt = this.user.encryptionSalt,
                            receivedFrom = senderUser
                        )
                        val decryptedAsset = DecryptedAssetImpl(
                            encryptedAsset.globalIdentifier,
                            encryptedAsset.localIdentifier,
                            encryptedAsset.creationDate,
                            decryptedVersions = mapOf(quality to decryptedData)
                        )

                        listeners.forEach { it.fetched(decryptedAsset)}

                        RemoteDownloadOperation.alreadyProcessed.add(descriptor.globalIdentifier)
                    }
                }
            }
        }
    }
}