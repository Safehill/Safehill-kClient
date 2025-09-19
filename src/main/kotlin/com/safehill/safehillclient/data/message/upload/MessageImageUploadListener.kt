package com.safehill.safehillclient.data.message.upload

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.tasks.outbound.OutboundQueueItem
import com.safehill.kclient.tasks.outbound.UploadFailure
import com.safehill.kclient.tasks.outbound.UploadOperationErrorListener
import com.safehill.safehillclient.data.message.model.MessageImageState
import com.safehill.safehillclient.data.threads.ThreadId
import com.safehill.safehillclient.data.threads.interactor.ThreadStateInteractor
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MessageImageUploadListener(
    private val scope: CoroutineScope,
    private val getThreadInteractor: suspend (ThreadId) -> ThreadStateInteractor
) : UploadOperationErrorListener {

    private val threadToGroupMapping = ConcurrentMap<ThreadId, List<GroupId>>()

    override fun enqueued(
        outboundQueueItem: OutboundQueueItem
    ) {
        scope.launch {
            val threadId = outboundQueueItem.threadId
            if (threadId != null) {
                val threadGroups = threadToGroupMapping.getOrElse(threadId) { listOf() }
                threadToGroupMapping[threadId] =
                    (threadGroups + outboundQueueItem.groupId).distinct()
                getThreadInteractor(threadId).updateImageStatus(
                    imageState = MessageImageState.Uploading(
                        localIdentifier = outboundQueueItem.localIdentifier,
                        globalIdentifier = outboundQueueItem.globalIdentifier
                    ),
                    groupId = outboundQueueItem.groupId
                )
            }
        }
    }

    override fun onError(
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        uploadFailure: UploadFailure
    ) {
        scope.launch {
            getThreadInteractorForGroup(groupId)?.updateImageStatus(
                imageState = MessageImageState.Failed(
                    localIdentifier = localIdentifier,
                    error = uploadFailure,
                    groupId = groupId,
                    globalIdentifier = globalIdentifier
                ),
                groupId = groupId
            )
        }
    }


    private suspend fun getThreadInteractorForGroup(groupId: GroupId): ThreadStateInteractor? {
        val threadId = threadToGroupMapping.entries
            .firstOrNull { (_, groupIds) -> groupIds.contains(groupId) }
            ?.key
        return threadId?.let { getThreadInteractor(it) }
    }

    override fun finishedSharing(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
        scope.launch {
            getThreadInteractorForGroup(groupId)?.updateImageStatus(
                imageState = MessageImageState.Completed(globalIdentifier),
                groupId = groupId
            )
        }
    }
}