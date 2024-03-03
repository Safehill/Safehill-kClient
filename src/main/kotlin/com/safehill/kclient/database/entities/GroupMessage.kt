package com.safehill.snoog.core.datastore.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "groups_messages")
data class GroupMessage(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "ref_asset_id") val refAssetId: UUID? = null,
    @ColumnInfo(name = "ref_group_message_id") val refGroupMessageId: UUID? = null,
    @ColumnInfo(name = "sender_id") val senderId: UUID,
    @ColumnInfo(name = "group_id") val groupId: String? = null,
    @ColumnInfo(name = "encrypted_message", typeAffinity = ColumnInfo.BLOB) val encryptedMessage: ByteArray,
    @ColumnInfo(name = "message_type") val messageType: String,
    @ColumnInfo(name = "time_created") val timeCreated: Date = Date(),
    @ColumnInfo(name = "time_updated") val timeUpdated: Date? = null,
    @ColumnInfo(name = "time_deleted") val timeDeleted: Date? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupMessage

        if (id != other.id) return false
        if (refAssetId != other.refAssetId) return false
        if (refGroupMessageId != other.refGroupMessageId) return false
        if (senderId != other.senderId) return false
        if (groupId != other.groupId) return false
        if (!encryptedMessage.contentEquals(other.encryptedMessage)) return false
        if (messageType != other.messageType) return false
        if (timeCreated != other.timeCreated) return false
        if (timeUpdated != other.timeUpdated) return false
        return timeDeleted == other.timeDeleted
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (refAssetId?.hashCode() ?: 0)
        result = 31 * result + (refGroupMessageId?.hashCode() ?: 0)
        result = 31 * result + senderId.hashCode()
        result = 31 * result + (groupId?.hashCode() ?: 0)
        result = 31 * result + encryptedMessage.contentHashCode()
        result = 31 * result + messageType.hashCode()
        result = 31 * result + timeCreated.hashCode()
        result = 31 * result + (timeUpdated?.hashCode() ?: 0)
        result = 31 * result + (timeDeleted?.hashCode() ?: 0)
        return result
    }
}