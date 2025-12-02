package com.safehill.safehillclient.data.collections.model

sealed class CollectionAccess {

    data object Unknown : CollectionAccess()


    data object Granted : CollectionAccess()

    data class PaymentRequired(
        val price: Double,
        val message: String? = null
    ) : CollectionAccess()


    data object Denied : CollectionAccess()
}