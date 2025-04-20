package com.safehill.safehillclient.module.platform

import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.local.LocalServerInterface
import com.safehill.kclient.tasks.outbound.LocalAssetGetter
import com.safehill.kclient.tasks.outbound.OutboundQueueItemManagerInterface
import com.safehill.kclient.utils.ImageResizerInterface
import com.safehill.safehillclient.utils.api.deviceid.DeviceIdProvider

interface PlatformModule {
    val deviceIdProvider: DeviceIdProvider
    val imageResizer: ImageResizerInterface
    val localAssetGetter: LocalAssetGetter
}

interface UserModule {
    fun getLocalServer(localUser: LocalUser): LocalServerInterface
    fun getOutboundQueueItemManager(localUser: LocalUser): OutboundQueueItemManagerInterface
}

