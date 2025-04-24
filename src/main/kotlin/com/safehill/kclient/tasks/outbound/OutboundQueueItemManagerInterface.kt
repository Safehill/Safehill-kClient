package com.safehill.kclient.tasks.outbound

interface OutboundQueueItemManagerInterface {
    fun loadOutboundQueueItems(): List<OutboundQueueItem>
    suspend fun removeOutboundQueueItem(queueItem: OutboundQueueItem)
    suspend fun addOutboundQueueItem(queueItem: OutboundQueueItem)
}
