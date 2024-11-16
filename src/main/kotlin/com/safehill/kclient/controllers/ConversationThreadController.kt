package com.safehill.kclient.controllers

import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.runCatchingPreservingCancellationException

class ConversationThreadController(
    val serverProxy: ServerProxy,
    val userInteractionController: UserInteractionController,
    val encryptionDetailsController: EncryptionDetailsController
) {

    suspend fun convertInvitees(
        threadId: String,
        invitees: List<ServerUser>
    ): Result<Unit> {
        return runCatchingPreservingCancellationException {
            val symmetricKey = userInteractionController.getSymmetricKey(
                anchorId = threadId,
                interactionAnchor = InteractionAnchor.THREAD
            )
            if (symmetricKey == null) {
                throw UserInteractionController.InteractionErrors.MissingE2EEDetails(
                    anchorId = threadId,
                    anchor = InteractionAnchor.THREAD
                )
            } else {
                serverProxy.convertInvitees(
                    threadIdWithEncryptionDetails = mapOf(
                        threadId to encryptionDetailsController.getRecipientEncryptionDetails(
                            users = invitees,
                            secretKey = symmetricKey
                        )
                    )
                )
            }
        }
    }
}