package com.safehill.kclient.tasks.outbound.sharing

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.assets.ShareableEncryptedAssetVersion
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.tasks.upload.RetryManager
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.kcrypto.models.ShareablePayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

interface SharingExecutor {
    fun execute(sharingRequest: SharingRequest): Flow<SharingState>
}

class DefaultSharingExecutor(
    private val retryManager: RetryManager,
    private val serverProxy: ServerProxy,
    private val localAssetsStoreController: LocalAssetsStoreController,
    private val userController: UserController,
) : SharingExecutor {

    private val user: LocalUser
        get() = serverProxy.requestor

    override fun execute(sharingRequest: SharingRequest): Flow<SharingState> {
        return flow {
            retryManager.executeWithRetry {
                executeSharingAttempt(sharingRequest)
            }.onSuccess {
                emit(SharingState.Success)
            }.onFailure { error ->
                if (error is SharingState.Failed) {
                    emit(error)
                } else {
                    emit(
                        SharingState.Failed(
                            error = error,
                            errorPhase = SharingState.Failed.Phase.UNKNOWN
                        )
                    )
                }
            }
        }
    }

    // todo maybe we can create a pipeline executor, that executes each step, pass the result to next step,
    // todo and emit the state in a robust way.
    private suspend fun FlowCollector<SharingState>.executeSharingAttempt(request: SharingRequest): Result<Unit> {
        return runCatchingSafe {

            val (sender, recipients) = runCatchingSafe {
                emit(SharingState.InProgress.GettingRecipients)
                getSenderAndRecipients(request)
            }.getOrElse { error ->
                throw SharingState.Failed(
                    error = error,
                    errorPhase = SharingState.Failed.Phase.GETTING_RECIPIENTS
                )
            }

            runCatchingSafe {
                emit(SharingState.InProgress.Sharing(recipients))
                val sharedVersions = getShareableEncryptedAssetVersions(
                    sharingRequest = request,
                    sender = sender,
                    recipients = recipients
                )
                serverProxy.share(
                    asset = ShareableEncryptedAsset(
                        globalIdentifier = request.globalIdentifier,
                        sharedVersions = sharedVersions,
                        groupId = request.groupId
                    ),
                    threadId = request.threadId
                )
            }.onFailure { error ->
                throw SharingState.Failed(
                    error = error,
                    errorPhase = SharingState.Failed.Phase.SHARING
                )
            }
        }
    }

    private suspend fun getSenderAndRecipients(sharingRequest: SharingRequest): Pair<ServerUser, List<ServerUser>> {
        val assetDescriptor = localAssetsStoreController.getAssetDescriptor(
            globalIdentifier = sharingRequest.globalIdentifier
        )
        val recipientIds = sharingRequest.recipients
        val senderUserIdentifier = assetDescriptor.sharingInfo.sharedByUserIdentifier
        val toFetchUsers = recipientIds + senderUserIdentifier
        val usersMap = userController.getUsers(toFetchUsers).getOrThrow()
        val sender = usersMap[senderUserIdentifier] ?: error("Asset Owner not found.")
        val recipients = recipientIds.mapNotNull { usersMap[it] }
        return sender to recipients
    }

    private suspend fun getShareableEncryptedAssetVersions(
        sharingRequest: SharingRequest,
        sender: ServerUser,
        recipients: List<ServerUser>
    ): List<ShareableEncryptedAssetVersion> {
        val encryptedAsset = serverProxy.getAsset(
            globalIdentifier = sharingRequest.globalIdentifier,
            qualities = sharingRequest.qualities,
            cacheAfterFetch = true
        )
        return recipients.flatMap { recipient ->
            sharingRequest.qualities.mapNotNull { quality ->
                val encryptedVersion =
                    encryptedAsset.encryptedVersions[quality] ?: return@mapNotNull null
                val sharedSecret = user.decryptSecret(
                    sealedMessage = ShareablePayload(
                        ephemeralPublicKeyData = encryptedVersion.publicKeyData,
                        ciphertext = encryptedVersion.encryptedSecret,
                        signature = encryptedVersion.publicSignatureData,
                        recipient = recipient
                    ),
                    protocolSalt = user.encryptionSalt,
                    sender = sender
                )
                val encryptedSharedSecretForRecipient = user.shareable(
                    data = sharedSecret,
                    with = recipient,
                    protocolSalt = user.encryptionSalt
                )
                ShareableEncryptedAssetVersion(
                    quality = quality,
                    userPublicIdentifier = recipient.identifier,
                    encryptedSecret = encryptedSharedSecretForRecipient.ciphertext,
                    ephemeralPublicKey = encryptedSharedSecretForRecipient.ephemeralPublicKeyData,
                    publicSignature = encryptedSharedSecretForRecipient.signature
                )
            }
        }
    }
}