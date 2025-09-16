package com.safehill.safehillclient.backgroundsync

import com.safehill.kclient.tasks.inbound.LocalDownloadOperation
import com.safehill.kclient.tasks.inbound.RemoteDownloadOperation
import com.safehill.kclient.tasks.outbound.upload.UploadOperation
import com.safehill.kclient.tasks.syncing.InteractionSync

class BackgroundTasksRegistry(
    val remoteDownloadOperation: RemoteDownloadOperation,
    val interactionSync: InteractionSync,
    val localDownloadOperation: LocalDownloadOperation,
    val uploadOperation: UploadOperation
)