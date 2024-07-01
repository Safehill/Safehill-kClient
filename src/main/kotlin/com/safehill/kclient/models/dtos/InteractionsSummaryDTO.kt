package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class InteractionsSummaryDTO(
    val summaryByThreadId: Map<String, InteractionsThreadSummaryDTO>,
    val summaryByGroupId: Map<String, InteractionsGroupSummaryDTO>
)

@Serializable
data class InteractionsThreadSummaryDTO(
    val thread: ConversationThreadOutputDTO,
    val lastEncryptedMessage: MessageOutputDTO?,
    val numMessages: Int,
    val numAssets: Int
)


@Serializable
data class InteractionsGroupSummaryDTO(
    val numComments: Int,
    val firstEncryptedMessage: MessageOutputDTO?,
    val reactions: List<ReactionOutputDTO>
)
