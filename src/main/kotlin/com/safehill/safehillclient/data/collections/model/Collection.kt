package com.safehill.safehillclient.data.collections.model

import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.collections.CollectionOutputDTO
import com.safehill.kclient.models.dtos.collections.CollectionVisibility
import com.safehill.kclient.models.users.UserIdentifier
import java.time.Instant
import java.util.Locale

/**
 * Domain model for a Collection.
 * Didn't use Collection because it was conflicting
 * with the already existing [kotlin.collections.Collection] on numerous imports
 */
data class CollectionModel(
    val id: String,
    val name: String,
    val description: String,
    val isSystemCollection: Boolean,
    val isArchived: Boolean,
    val assetCount: Int,
    val visibility: CollectionVisibility,
    val pricing: Double,
    val lastUpdated: Instant,
    val createdBy: UserIdentifier,
    val assets: List<AssetOutputDTO>
) {

    /**
     * Whether this collection is free to access
     */
    val isFree: Boolean
        get() = pricing == 0.0

    /**
     * Whether this collection requires payment
     */
    val isPaid: Boolean
        get() = pricing > 0.0

    /**
     * Whether this collection is publicly discoverable
     */
    val isPublic: Boolean
        get() = visibility == CollectionVisibility.PUBLIC

    /**
     * Whether this collection is confidential (requires link or payment)
     */
    val isConfidential: Boolean
        get() = visibility == CollectionVisibility.CONFIDENTIAL

    /**
     * Whether this collection is private (not shared)
     */
    val isPrivate: Boolean
        get() = visibility == CollectionVisibility.NOT_SHARED

    /**
     * Check if the user owns this collection
     */
    fun isOwnedBy(userId: String): Boolean {
        return createdBy == userId
    }

    /**
     * Get the first asset ID for preview purposes
     * Prefers public assets over private assets
     */
    val previewAsset: AssetOutputDTO?
        get() = assets.firstOrNull { it.isPublic == true } ?: assets.firstOrNull()

    val roundedPricing: String
        get() = String.format(Locale.getDefault(), "%.2f", pricing)


}

fun CollectionOutputDTO.toCollection(): CollectionModel {
    return CollectionModel(
        id = id,
        name = name,
        description = description,
        isSystemCollection = isSystemCollection,
        isArchived = isArchived ?: false,
        assetCount = assetCount,
        visibility = visibility,
        pricing = pricing,
        lastUpdated = lastUpdated,
        createdBy = createdBy,
        assets = assets
    )
}

fun randomCollectionGenerator(): CollectionModel {
    val assets = mutableListOf<AssetOutputDTO>()
    return CollectionModel(
        id = "coll_${System.currentTimeMillis()}",
        name = "Fake Collection",
        description = "This is a long and detailed description for a fake collection that is generated for preview and testing purposes.",
        isSystemCollection = false,
        isArchived = false,
        assetCount = assets.size,
        visibility = CollectionVisibility.PUBLIC,
        pricing = 19.99,
        lastUpdated = Instant.now(),
        createdBy = "user_123",
        assets = assets
    )
}
