package com.safehill.kclient.tasks.upload

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.tasks.outbound.AssetEncrypter
import com.safehill.kclient.tasks.outbound.OutboundQueueItem
import com.safehill.kclient.tasks.outbound.UploadListenersRegistry
import com.safehill.kclient.tasks.outbound.model.UploadExecutor
import com.safehill.kclient.tasks.outbound.model.UploadRequest
import com.safehill.kclient.tasks.outbound.model.UploadState
import com.safehill.kclient.tasks.outbound.model.UploadState.InProgress
import com.safehill.kclient.tasks.outbound.model.correspondingErrorPhase
import com.safehill.kclient.util.runCatchingSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class RobustUploadManager(
    private val retryManager: RetryManager = DefaultRetryManager(ExponentialBackoffRetryPolicy()),
    private val serverProxy: ServerProxy,
    private val encrypter: AssetEncrypter,
    private val localAssetsStoreController: LocalAssetsStoreController,
    private val safehillLogger: SafehillLogger,
    private val uploadListenersRegistry: UploadListenersRegistry
) : UploadExecutor {

    private val user: LocalUser get() = serverProxy.requestor

    override suspend fun execute(request: UploadRequest): Flow<UploadState> {
        return flow {
            retryManager.executeWithRetry { attempt ->
                executeSingleUploadAttempt(request)
            }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun FlowCollector<UploadState>.executeSingleUploadAttempt(
        request: UploadRequest,
    ): Result<Unit> {
        val outboundQueueItem = convertToOutboundQueueItem(request)
        return runCatchingSafe {
            uploadListenersRegistry.notifyListenersStartedEncrypting(request)
            val encryptedAsset = runUploadStep(
                progressState = InProgress.Encrypting,
                action = { encrypter.encryptedAsset(outboundQueueItem, user) }
            )
            uploadListenersRegistry.notifyListenersFinishedEncrypting(request)


            uploadListenersRegistry.notifyListenersStartedUploading(request)
            runUploadStep(
                progressState = InProgress.Uploading,
                action = {
                    serverProxy.remoteServer.upload(
                        listOf(encryptedAsset),
                        outboundQueueItem.groupId
                    )
                }
            )
            uploadListenersRegistry.notifyListenersFinishedUploading(request)
        }
    }


    private suspend inline fun <T> FlowCollector<UploadState>.runUploadStep(
        progressState: InProgress,
        action: () -> T
    ): T {
        return runCatchingSafe {
            emit(progressState)
            action()
        }.onFailure { error ->
            val failedState = UploadState.Failed(
                error = error,
                phase = progressState.correspondingErrorPhase
            )
            emit(failedState)
        }.getOrThrow()
    }

    private fun convertToOutboundQueueItem(request: UploadRequest): OutboundQueueItem {
        return OutboundQueueItem(
            operationType = OutboundQueueItem.OperationType.Upload,
            assetQualities = request.qualities,
            globalIdentifier = request.globalIdentifier,
            localIdentifier = request.localIdentifier,
            groupId = request.groupId,
            recipientIds = request.recipients,
            threadId = request.threadId,
            operationState = OutboundQueueItem.OperationState.Enqueued
        )
    }
}