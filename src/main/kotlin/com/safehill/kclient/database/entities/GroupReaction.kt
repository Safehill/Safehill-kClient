package com.safehill.snoog.core.datastore.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "groups_reactions")
data class GroupReaction(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "ref_asset_id") val refAssetId: UUID? = null,
    @ColumnInfo(name = "ref_group_message_id") val refGroupMessageId: UUID? = null,
    @ColumnInfo(name = "sender_id") val senderId: UUID,
    @ColumnInfo(name = "group_id") val groupId: String? = null,
    @ColumnInfo(name = "reaction_type") val reactionType: String,
    @ColumnInfo(name = "time_created") val timeCreated: Date = Date(),
    @ColumnInfo(name = "time_updated") val timeUpdated: Date? = null,
    @ColumnInfo(name = "time_deleted") val timeDeleted: Date? = null,
    @ColumnInfo(name = "group_thread_id") val groupThreadId: UUID? = null,
    @ColumnInfo(name = "conversation_thread_id") val conversationThreadId: UUID? = null,
)
