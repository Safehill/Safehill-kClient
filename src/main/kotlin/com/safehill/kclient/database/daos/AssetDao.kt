package com.safehill.kclient.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.safehill.snoog.core.datastore.database.entities.relationships.AssetWithAssetVersion

@Dao
interface AssetDao {
    @Transaction
    @Query("SELECT * FROM assets")
    suspend fun getAll(): List<AssetWithAssetVersion>
}
