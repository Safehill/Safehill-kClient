package com.safehill.kclient.network.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ConversationThreadOutputDTO(
    val threadId: String,
    val name: String?,
    val membersPublicIdentifier: List<String>,
    val lastUpdatedAt: String,
    val encryptionDetails: RecipientEncryptionDetailsDTO // for the user making the request
)
