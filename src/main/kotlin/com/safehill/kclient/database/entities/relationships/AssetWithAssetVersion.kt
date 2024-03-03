package com.safehill.snoog.core.datastore.database.entities.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.safehill.snoog.core.datastore.database.entities.Asset
import com.safehill.snoog.core.datastore.database.entities.AssetVersion

data class AssetWithAssetVersion(
    @Embedded val asset: Asset,
    @Relation(
        parentColumn = "id",
        entityColumn = "asset_id"
    )
    val versions: List<AssetVersion>
)
