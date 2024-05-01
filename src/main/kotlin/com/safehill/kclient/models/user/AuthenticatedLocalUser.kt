package com.safehill.kclient.models.user

import com.safehill.kclient.models.*
import java.util.function.Consumer

/**
 * An immutable version of the `SHLocalUser` after it's been authenticated
 * where `encryptionProtocolSalt` and `authToken` is guaranteed to be set.
 */
class AuthenticatedLocalUser(
    var localUser: SHLocalUser,
    override var name: String,
    override var maybeEncryptionProtocolSalt: ByteArray,
    override var authToken: String?,
    override var keychainPrefix: String) : SHLocalUserInterface {
    constructor(localUser: SHLocalUser, name: String) : this(localUser, name, localUser.maybeEncryptionProtocolSalt, localUser.authToken, localUser.keychainPrefix) {
        require(!(localUser.maybeEncryptionProtocolSalt == null || localUser.authToken == null)) { "Encryption protocol salt and auth token must be set" }
    }

    fun shareableEncryptedAsset(
        globalIdentifier: String?,
        versions: List<SHAssetQuality?>,
        recipients: List<SHServerUser>,
        groupId: String?,
        completionHandler: Consumer<Result<SHShareableEncryptedAsset?>?>
    ) {
        val localAssetStoreController = SHLocalAssetStoreController(this)
        localAssetStoreController.retrieveCommonEncryptionKey(globalIdentifier) { result ->
            if (result.isFailure()) {
                completionHandler.accept(Result.failure(result.getError()))
            } else {
                val privateSecret: ByteArray = result.getSuccess()
                val shareableVersions: MutableList<SHShareableEncryptedAssetVersion> =
                    ArrayList()
                for (recipient in recipients) {
                    try {
                        val encryptedAssetSecret: EncryptedAssetSecret? =
                            createShareablePayload(privateSecret, recipient)
                        for (quality in versions) {
                            val shareableVersion =
                                SHGenericShareableEncryptedAssetVersion(
                                    quality,
                                    recipient.identifier,
                                    encryptedAssetSecret.getCyphertext(),
                                    encryptedAssetSecret.getEphemeralPublicKeyData(),
                                    encryptedAssetSecret.getSignature()
                                )
                            shareableVersions.add(shareableVersion)
                        }
                    } catch (e: Exception) {
                        completionHandler.accept(Result.failure(e))
                        return@retrieveCommonEncryptionKey
                    }
                }
                val shareableEncryptedAsset =
                    SHGenericShareableEncryptedAsset(globalIdentifier, shareableVersions, groupId)
                completionHandler.accept(Result.success(shareableEncryptedAsset))
            }
        }
    }

    @Throws(Exception::class)
    private fun createShareablePayload(privateSecret: ByteArray, recipient: SHServerUser): EncryptedAssetSecret? {
        // Implement createShareablePayload method
        return null
    }
}
