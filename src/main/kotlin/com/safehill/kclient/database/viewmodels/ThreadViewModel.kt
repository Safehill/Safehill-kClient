package com.safehill.snoog.core.datastore.database.viewmodels

import androidx.lifecycle.ViewModel
import com.safehill.snoog.core.datastore.database.entities.ConversationThread
import com.safehill.kclient.database.repositories.ThreadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val threadRepository: ThreadRepository
) : ViewModel() {

    suspend fun getAllConversationThreads(): List<ConversationThread> {
        return threadRepository.getAllConversationThreads()
    }

    suspend fun insertConversationThread(conversationThread: ConversationThread) {
        threadRepository.insertConversationThread(conversationThread)
    }

    fun deleteConversationThread(conversationThread: ConversationThread) {
        threadRepository.deleteConversationThread(conversationThread)
    }
}