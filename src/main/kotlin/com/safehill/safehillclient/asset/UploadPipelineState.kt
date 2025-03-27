package com.safehill.safehillclient.asset

import com.safehill.kclient.models.users.ServerUser

sealed class UploadPipelineState(open val users: List<ServerUser>?) {
    data object Encrypting : UploadPipelineState(null)
    data object Encrypted : UploadPipelineState(null)
    data object FailedEncrypting : UploadPipelineState(null)
    data object Uploading : UploadPipelineState(null)
    data object Uploaded : UploadPipelineState(null)
    data object FailedUploading : UploadPipelineState(null)
    data class Sharing(override val users: List<ServerUser>) : UploadPipelineState(users)
    data class Shared(override val users: List<ServerUser>) : UploadPipelineState(users)
    data class FailedSharing(override val users: List<ServerUser>) : UploadPipelineState(users)

    //TODO Correct value
    fun info(): String {
        return when (this) {
            is Encrypting -> "Encrypting"
            is Encrypted -> "Encrypted"
            is FailedEncrypting -> "Failed on encrypting"
            is Uploading -> "Uploading"
            is Uploaded -> "Uploaded"
            is FailedUploading -> "Failed on uploading"
            is Sharing -> "Sharing with ${users.joinToString(", ") { it.name }}"
            is Shared -> "Shared with ${users.joinToString(", ") { it.name }}"
            is FailedSharing -> "Failed on sharing with ${users.joinToString(", ") { it.name }}"
        }
    }

    fun waitTime(): Long? {
        return when (this) {
            is Encrypting -> null
            is Encrypted -> null
            is FailedEncrypting -> 2000
            is Uploading -> null
            is Uploaded -> 2000
            is FailedUploading -> 2000
            is Sharing -> null
            is Shared -> 2000
            is FailedSharing -> 2000
        }
    }
}