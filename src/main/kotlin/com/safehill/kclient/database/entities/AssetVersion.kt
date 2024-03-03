package com.safehill.snoog.core.datastore.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "assets_versions")
data class AssetVersion(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "asset_id") val assetId: UUID,
    @ColumnInfo(name = "version_name") val versionName: String,
    @ColumnInfo(name = "time_queued") val timeQueued: Date? = null,
    @ColumnInfo(name = "time_updated") val timeUpdated: Date? = null,
)