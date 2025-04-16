package com.safehill.safehillclient.data.message.model

import com.safehill.safehillclient.utils.api.paginateable.DefaultPaginateable
import com.safehill.safehillclient.utils.api.paginateable.Paginateable
import com.safehill.utils.flow.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

fun MutableMessagesContainer(): MutableMessagesContainer {
    return MutableMessagesContainerImpl()
}

private class MutableMessagesContainerImpl : MutableMessagesContainer,
    Paginateable by DefaultPaginateable() {

    private val _messages: MutableStateFlow<Map<String, Message>> = MutableStateFlow(mapOf())
    override val messages: StateFlow<List<Message>> = _messages.mapState { messageMap ->
        messageMap
            .values
            .sortedBy { it.createdDate }
            .toList()
    }

    private val _lastActiveDate: MutableStateFlow<Instant?> = MutableStateFlow(null)
    override val lastActiveDate: StateFlow<Instant?> = _lastActiveDate.asStateFlow()

    override fun upsertMessages(message: List<Message>) {
        val newMessages = message.associateBy { it.id }
        _messages.update { initial ->
            initial + newMessages
        }
    }

    override fun setLastUpdatedAt(instant: Instant) {
        _lastActiveDate.update { instant }
    }

    override fun upsertMessage(message: Message) {
        upsertMessages(listOf(message))
    }

    override fun updateMessage(localID: String, message: Message) {
        setMessages(
            messages.value.map {
                if (it.id == localID) message else it
            }
        )
    }

    private fun setMessages(messages: List<Message>) {
        _messages.update { messages.associateBy { it.id } }
    }
}