package com.safehill.safehillclient.data.message.model

import com.safehill.safehillclient.utils.api.paginateable.Paginateable
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

interface MessagesContainer : Paginateable {
    val messages: StateFlow<List<Message>>
    val lastActiveDate: StateFlow<Instant?>
}

interface MutableMessagesContainer : MessagesContainer {

    fun upsertMessages(message: List<Message>)

    fun upsertMessage(message: Message)

    fun updateMessage(localID: String, message: Message)

    fun setLastUpdatedAt(instant: Instant)

    fun modifyMessages(update: (Map<String, Message>) -> Map<String, Message>)
}