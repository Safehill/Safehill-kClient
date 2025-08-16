package com.safehill.safehillclient.module.config

import com.safehill.kclient.logging.DefaultSafehillLogger
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.safehillclient.ClientScope
import com.safehill.safehillclient.device_registration.DeviceRegistrationStrategy
import com.safehill.safehillclient.utils.api.dispatchers.SdkDispatchers
import com.safehill.safehillclient.utils.extensions.createChildScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Configs(
    val deviceRegistrationStrategy: DeviceRegistrationStrategy,
    val postAssetEmbeddings: Boolean
)

class ClientOptions(
    val safehillLogger: SafehillLogger = DefaultSafehillLogger(),
    val sdkDispatchers: SdkDispatchers = SdkDispatchers(
        io = Dispatchers.IO,
        default = Dispatchers.Default
    ),
    val clientScope: ClientScope = CoroutineScope(
        SupervisorJob() + sdkDispatchers.io + CoroutineExceptionHandler { coroutineContext, throwable ->
            safehillLogger.error("Exception in coroutine scope: coroutineContext=$coroutineContext, throwable=$throwable")
        }
    ),
    val userScope: CoroutineScope = clientScope.createChildScope { SupervisorJob(it) }
)