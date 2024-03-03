package com.safehill.snoog.core.datastore.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.safehill.snoog.core.datastore.database.entities.ConversationThread
import java.util.UUID

@Dao
interface ConversationThreadDao {
    @Query("SELECT * FROM conversation_threads")
    suspend fun getAll(): List<ConversationThread>

    @Query("SELECT * FROM conversation_threads WHERE id IN (:threadIds)")
    fun loadAllByIds(threadIds: Array<String>): List<ConversationThread>

    @Query("SELECT * FROM conversation_threads WHERE id LIKE :id LIMIT 1")
    suspend fun findByName(id: UUID): ConversationThread

    @Insert
    suspend fun insertAll(vararg threads: ConversationThread)

    @Delete
    fun delete(thread: ConversationThread)
}
