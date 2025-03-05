package com.safehill.safehillclient.sdk.utils.api.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

data class SdkDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
)