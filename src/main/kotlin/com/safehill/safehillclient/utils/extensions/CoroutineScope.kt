package com.safehill.safehillclient.utils.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import kotlin.coroutines.CoroutineContext

fun CoroutineScope.cancelChildren() {
    this.coroutineContext.job.cancelChildren()
}


fun CoroutineScope.createChildScope(
    block: (Job) -> CoroutineContext
) = this + block(this.coroutineContext.job)