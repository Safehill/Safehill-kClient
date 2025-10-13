package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.safehillclient.SafehillClient
import com.safehill.safehillclient.utils.extensions.backgroundTasksRegistry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AssetOutboundItemAwaitManager(
    private val uploadOperation: UploadOperation,
    private val safehillLogger: SafehillLogger
) {

    suspend fun await(
        outboundAwaitingMode: OutboundAwaitingMode
    ) {
        coroutineScope {
            when (outboundAwaitingMode) {
                OutboundAwaitingMode.None -> {}
                is OutboundAwaitingMode.Share -> {
                    outboundAwaitingMode.globalIdentifiers.forEach { identifier ->
                        launch {
                            awaitShare(identifier = identifier)
                        }
                    }
                }

                is OutboundAwaitingMode.Upload -> {
                    outboundAwaitingMode.localIdentifiers.forEach { identifier ->
                        launch {
                            awaitUpload(identifier = identifier)
                        }
                    }
                }
            }
        }
    }

    suspend fun awaitUpload(
        identifier: AssetLocalIdentifier
    ) {
        // Cannot use suspendableCancellableCoroutine because the upload listener is not single shot.
        // Further information can be found in the following thread.
        // https://github.com/Kotlin/kotlinx.coroutines/issues/3065
        callbackFlow {
            val producerScope = this
            val listener = object : UploadOperationListener {
                override fun finishedUploading(
                    localIdentifier: AssetLocalIdentifier,
                    globalIdentifier: AssetGlobalIdentifier,
                    groupId: GroupId
                ) {
                    if (localIdentifier == identifier) {
                        producerScope.launch { send(Unit) }
                    }
                }

                override fun failedUploading(
                    globalIdentifier: AssetGlobalIdentifier,
                    localIdentifier: AssetLocalIdentifier,
                    groupId: GroupId
                ) {
                    if (localIdentifier == identifier) {
                        producerScope.close(Exception("Failed uploading."))
                    }
                }
            }
            uploadOperation.listeners.add(listener)
            awaitClose {
                uploadOperation.listeners.remove(listener)
            }
        }.first()
    }

    suspend fun awaitShare(
        identifier: AssetGlobalIdentifier
    ) {
        //todo await sharing.
    }
}

val SafehillClient.assetOutboundItemAwaitManager
    get() = AssetOutboundItemAwaitManager(
        uploadOperation = backgroundTasksRegistry.uploadOperation,
        safehillLogger = this.clientModule.clientOptions.safehillLogger
    )

sealed interface OutboundAwaitingMode {
    class Upload(val localIdentifiers: List<AssetLocalIdentifier>) : OutboundAwaitingMode
    class Share(val globalIdentifiers: List<AssetGlobalIdentifier>) : OutboundAwaitingMode
    object None : OutboundAwaitingMode
}