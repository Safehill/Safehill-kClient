package com.safehill.kclient.tasks.upload.queue

import com.safehill.kclient.tasks.upload.RetryPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

class ChannelQueue<T>(
    capacity: Int = Channel.UNLIMITED,
    private val processor: ItemProcessor<T>,
    private val scope: CoroutineScope,
    private val retryPolicy: RetryPolicy
) : Queue<T> {

    private val channel = Channel<QueueItem<T>>(capacity)

    override suspend fun enqueue(item: T) {
        val queueItem = QueueItem(
            item = item,
            retryCount = 0
        )
        enqueue(queueItem)
    }

    private suspend fun enqueue(queueItem: QueueItem<T>) {
        processor.onEnqueued(queueItem.item)
        channel.send(queueItem)
    }

    fun startProcessingLoop() {
        scope.launch {
            for (queueItem in channel) {
                processor
                    .process(queueItem.item)
                    .onFailure { error ->
                        val toRetryQueueItem = queueItem.copy(
                            retryCount = queueItem.retryCount + 1
                        )
                        val shouldRetry = retryPolicy.shouldRetry(
                            attempt = toRetryQueueItem.retryCount,
                            error = error
                        )
                        if (shouldRetry) {
                            sendToChannelForRetrying(queueItem)
                        }
                    }
            }
        }
    }

    private fun CoroutineScope.sendToChannelForRetrying(
        queueItem: QueueItem<T>
    ) {
        launch {
            val delayDuration =
                minOf(30.seconds, 2.0.pow(queueItem.retryCount).seconds)
            delay(delayDuration)
            enqueue(queueItem)
        }
    }
}
