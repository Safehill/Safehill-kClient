package com.safehill.snoog.core.datastore.database

import androidx.room.Database
import androidx.room.TypeConverters
import com.safehill.snoog.core.datastore.database.entities.ConversationThread
import com.safehill.snoog.core.datastore.database.daos.ConversationThreadDao

@Database(
    entities = [ConversationThread::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : androidx.room.Database() {
    abstract fun conversationThreadDao(): ConversationThreadDao
}