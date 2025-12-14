package com.safehill.safehillclient.data.collections.mappers

import com.safehill.kclient.models.dtos.collections.AccessCheckResultDTO
import com.safehill.kclient.models.dtos.collections.AccessStatus
import com.safehill.kclient.models.dtos.collections.CollectionOutputDTO
import com.safehill.safehillclient.data.collections.model.CollectionAccess
import com.safehill.safehillclient.data.collections.model.CollectionModel

fun CollectionOutputDTO.toCollection(
    access: CollectionAccess
): CollectionModel {
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
        assets = assets,
        access = access
    )
}


fun CollectionModel.toDto(): CollectionOutputDTO {
    return CollectionOutputDTO(
        id = id,
        name = name,
        description = description,
        isSystemCollection = isSystemCollection,
        isArchived = isArchived,
        assetCount = assetCount,
        visibility = visibility,
        pricing = pricing,
        lastUpdated = lastUpdated,
        createdBy = createdBy,
        assets = assets
    )
}

fun AccessCheckResultDTO.toCollectionAccess(): CollectionAccess {
    return when (status) {
        AccessStatus.GRANTED -> CollectionAccess.Granted
        AccessStatus.PAYWALL -> CollectionAccess.PaymentRequired(
            price = price ?: 0.0,
            message = message
        )

        AccessStatus.DENIED, AccessStatus.LOADING -> CollectionAccess.Denied
    }
}
