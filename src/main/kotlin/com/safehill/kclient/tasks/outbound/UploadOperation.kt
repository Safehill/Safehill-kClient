package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.users.LocalUser

public interface UploadOperation {

    val listeners: List<UploadOperationListener>

    val user: LocalUser

    suspend fun upload(outboundQueueItem: OutboundQueueItem)

    suspend fun share(outboundQueueItem: OutboundQueueItem)

}