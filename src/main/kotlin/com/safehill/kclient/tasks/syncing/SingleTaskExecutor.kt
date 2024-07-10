package com.safehill.kclient.tasks.syncing

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class SingleTaskExecutor {

    private var currentJob: Job? = null

    suspend fun execute(block: suspend () -> Unit) {
        currentJob?.cancel()
        coroutineScope {
            currentJob = launch { block() }
        }
    }

}