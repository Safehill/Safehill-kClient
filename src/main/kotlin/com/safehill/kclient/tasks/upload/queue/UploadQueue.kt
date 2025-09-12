package com.safehill.kclient.tasks.upload.queue


interface ItemProcessor<T> {

    suspend fun onEnqueued(item: T)

    suspend fun process(item: T): Result<Unit>

}


interface Queue<T> {

    suspend fun enqueue(
        item: T
    )

}

data class QueueItem<T>(
    val item: T,
    val retryCount: Int
)