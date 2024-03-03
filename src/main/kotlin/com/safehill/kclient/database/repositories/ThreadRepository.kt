package com.safehill.kclient.database.repositories

import com.safehill.snoog.core.datastore.database.daos.ConversationThreadDao
import com.safehill.snoog.core.datastore.database.entities.ConversationThread

class ThreadRepository(
    private val conversationThreadDao: ConversationThreadDao
) {

    suspend fun getAllConversationThreads(): List<ConversationThread> {
        return conversationThreadDao.getAll()
    }

    suspend fun insertConversationThread(conversationThread: ConversationThread) {
        conversationThreadDao.insertAll(conversationThread)
    }

    fun deleteConversationThread(conversationThread: ConversationThread) {
        conversationThreadDao.delete(conversationThread)
    }
}