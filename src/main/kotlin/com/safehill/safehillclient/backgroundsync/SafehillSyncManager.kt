package com.safehill.safehillclient.backgroundsync

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.tasks.BackgroundTaskProcessor
import com.safehill.kclient.tasks.RepeatMode
import com.safehill.kclient.tasks.inbound.LocalDownloadOperation
import com.safehill.kclient.tasks.inbound.RemoteDownloadOperation
import com.safehill.kclient.tasks.outbound.UploadOperationListener
import com.safehill.kclient.tasks.outbound.upload.UploadOperation
import com.safehill.kclient.tasks.syncing.InteractionSync
import com.safehill.kclient.tasks.syncing.SingleTaskExecutor
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.client.UserScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class SafehillSyncManager(
    private val backgroundTasksRegistry: BackgroundTasksRegistry,
    private val userScope: UserScope
) : UploadOperationListener, UserObserver {

    private val singleTaskExecutor = SingleTaskExecutor()

    private val remoteDownloadProcessor: BackgroundTaskProcessor<RemoteDownloadOperation> =
        BackgroundTaskProcessor()

    private val remoteUploadProcessor: BackgroundTaskProcessor<UploadOperation> =
        BackgroundTaskProcessor()

    private val localDownloadProcessor: BackgroundTaskProcessor<LocalDownloadOperation> =
        BackgroundTaskProcessor()

    private val interactionSyncProcessor: BackgroundTaskProcessor<InteractionSync> =
        BackgroundTaskProcessor()

    init {
        backgroundTasksRegistry.uploadOperation.listeners.add(this)
    }

    private fun startBackgroundSync() {
        userScope.launch {
            launch {
                remoteDownloadProcessor.run(
                    task = backgroundTasksRegistry.remoteDownloadOperation,
                    repeatMode = RepeatMode.Repeating(interval = 5.seconds)
                )
            }
            launch {
                remoteUploadProcessor.run(
                    task = backgroundTasksRegistry.uploadOperation,
                    repeatMode = RepeatMode.Repeating(interval = 30.seconds)
                )
            }
            launch {
                interactionSyncProcessor.run(
                    task = backgroundTasksRegistry.interactionSync
                )
            }

            launch {
                localDownloadProcessor.run(
                    task = backgroundTasksRegistry.localDownloadOperation,
                    repeatMode = RepeatMode.Repeating(interval = 30.seconds)
                )
            }
        }
    }

    override fun finishedUploading(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
    ) {
        //TODO: better scope handling
        GlobalScope.launch {
            singleTaskExecutor.execute {
                backgroundTasksRegistry.remoteDownloadOperation.run()
            }
        }
    }

    override suspend fun userLoggedIn(user: LocalUser) {
        startBackgroundSync()
    }

    override fun userLoggedOut() {}

}