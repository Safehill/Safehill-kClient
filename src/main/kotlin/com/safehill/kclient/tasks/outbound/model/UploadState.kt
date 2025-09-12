package com.safehill.kclient.tasks.outbound.model

import com.safehill.kclient.models.users.ServerUser

/**
 * Represents the various states of an upload operation using sealed classes for type safety
 * Covers the complete upload pipeline: Queue -> Encrypt -> Upload -> Share -> Complete
 */
sealed class UploadState {

    /**
     * Upload is queued and waiting to be processed
     */
    data object Pending : UploadState()

    /**
     * Upload is being processed - base class for all active states
     */
    sealed class InProgress() : UploadState() {

        /**
         * Asset is being encrypted before upload
         */
        data object Encrypting : InProgress()

        /**
         * Encrypted asset is being uploaded to server
         */
        data object Uploading : InProgress()

        /**
         * Asset is being shared with recipients after successful upload
         */
        data class Sharing(
            val recipients: List<ServerUser> = emptyList()
        ) : InProgress()

    }

    /**
     * Upload completed successfully (including sharing if applicable)
     */
    data object Success : UploadState()

    /**
     * Upload failed with error details and retry information
     */
    data class Failed(
        val error: Throwable,
        val phase: FailurePhase = FailurePhase.UNKNOWN
    ) : UploadState() {

        enum class FailurePhase {
            ENCRYPTION, UPLOAD, SHARING, UNKNOWN
        }
    }

    /**
     * Upload has been cancelled by user
     */
    data object Cancelled : UploadState()
}

val UploadState.InProgress.correspondingErrorPhase
    get() = when (this) {
        UploadState.InProgress.Encrypting -> UploadState.Failed.FailurePhase.ENCRYPTION
        is UploadState.InProgress.Sharing -> UploadState.Failed.FailurePhase.SHARING
        UploadState.InProgress.Uploading -> UploadState.Failed.FailurePhase.UPLOAD
    }