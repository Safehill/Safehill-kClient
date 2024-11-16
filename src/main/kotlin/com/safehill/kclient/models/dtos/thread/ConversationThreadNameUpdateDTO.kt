package com.safehill.kclient.models.dtos.thread;

import kotlinx.serialization.Serializable

@Serializable
data class ConversationThreadNameUpdateDTO(
    val name: String?
)