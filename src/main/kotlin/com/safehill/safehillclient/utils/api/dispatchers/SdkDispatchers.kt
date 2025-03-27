package com.safehill.safehillclient.utils.api.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

data class SdkDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
)