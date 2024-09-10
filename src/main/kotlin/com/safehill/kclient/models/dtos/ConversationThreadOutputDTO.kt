package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ConversationThreadOutputDTO(
    val threadId: String,
    val name: String?,
    val membersPublicIdentifier: List<String>,
    val invitedUsersPhoneNumbers: Map<String, @Serializable(with = InstantSerializer::class) Instant>,
    @Serializable(with = InstantSerializer::class) val lastUpdatedAt: Instant,
    val creatorPublicIdentifier: String,
    val createdAt: String,
    val encryptionDetails: RecipientEncryptionDetailsDTO // for the user making the request
)