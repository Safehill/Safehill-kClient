package com.safehill.safehillclient.platform

import com.safehill.kclient.logging.DefaultSafehillLogger
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.local.EncryptionHelper
import com.safehill.kclient.network.local.LocalServerInterface
import com.safehill.kclient.tasks.outbound.OutboundQueueItemManagerInterface
import com.safehill.kclient.utils.ImageResizerInterface
import com.safehill.safehillclient.utils.api.deviceid.DeviceIdProvider

interface PlatformModule {
    val deviceIdProvider: DeviceIdProvider
    val imageResizer: ImageResizerInterface
    val safehillLogger: SafehillLogger
        get() = DefaultSafehillLogger()
}

interface UserModule {
    fun getLocalServer(localUser: LocalUser): LocalServerInterface
    fun getEncryptionHelper(localUser: LocalUser): EncryptionHelper
    fun getOutboundQueueItemManager(localUser: LocalUser): OutboundQueueItemManagerInterface
}

