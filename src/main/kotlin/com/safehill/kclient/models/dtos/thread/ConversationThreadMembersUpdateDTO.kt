package com.safehill.kclient.models.dtos.thread

import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import kotlinx.serialization.Serializable

@Serializable
data class ConversationThreadMembersUpdateDTO(
    val recipientsToAdd: List<RecipientEncryptionDetailsDTO>,
    val membersPublicIdentifierToRemove: List<String>,
    val phoneNumbersToAdd: List<String>,
    val phoneNumbersToRemove: List<String>
)