package com.safehill.safehillclient.data.collections.model

import kotlinx.serialization.Serializable

@Serializable
sealed class CollectionAccess {
    @Serializable
    data object Unknown : CollectionAccess()

    @Serializable
    data object Granted : CollectionAccess()

    @Serializable
    data class PaymentRequired(
        val price: Double,
        val message: String? = null
    ) : CollectionAccess()

    @Serializable
    data object Denied : CollectionAccess()
}