package com.safehill.kclient.api.dtos.response

import com.safehill.kclient.api.dtos.SHRecipientEncryptionDetailsDTO
import kotlinx.serialization.Serializable

@Serializable
data class ConversationThreadOutputDTO(
    val threadId: String,
    val name: String?,
    val membersPublicIdentifier: List<String>,
    val lastUpdatedAt: String?,
    val encryptionDetails: SHRecipientEncryptionDetailsDTO
)