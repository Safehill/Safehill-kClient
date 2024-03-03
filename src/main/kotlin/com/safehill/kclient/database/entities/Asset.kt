package com.safehill.snoog.core.datastore.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "global_identifier") val globalIdentifier: String,
    @ColumnInfo(name = "local_identifier") val localIdentifier: String? = null,
    @ColumnInfo(name = "sender_id") val senderId: UUID,
    @ColumnInfo(name = "asset_creation_date") val assetCreationDate: Date,
    @ColumnInfo(name = "time_created") val timeCreated: Date,
    @ColumnInfo(name = "time_updated") val timeUpdated: Date? = null,
    @ColumnInfo(name = "time_deleted") val timeDeleted: Date? = null,
)

