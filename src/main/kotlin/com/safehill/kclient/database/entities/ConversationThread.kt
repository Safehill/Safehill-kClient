package com.safehill.snoog.core.datastore.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "conversation_threads")
data class ConversationThread(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String? = null,
    @ColumnInfo(name = "last_interaction_at") val lastInteractionAt: Date? = null,
    @ColumnInfo(name = "time_created") val timeCreated: Date = Date(),
    @ColumnInfo(name = "time_updated") val timeUpdated: Date? = null,
    @ColumnInfo(name = "time_deleted") val timeDeleted: Date? = null,
)

