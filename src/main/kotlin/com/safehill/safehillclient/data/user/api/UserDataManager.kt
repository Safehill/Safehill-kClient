package com.safehill.safehillclient.data.user.api

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.module.platform.UserModule
import com.safehill.safehillclient.utils.api.dispatchers.SdkDispatchers
import kotlinx.coroutines.withContext

class UserDataManager(
    private val userModule: UserModule,
    private val sdkDispatchers: SdkDispatchers
) {

    suspend fun clear(localUser: LocalUser) {
        withContext(sdkDispatchers.io) {
            userModule.getLocalServer(localUser).clear()
        }
    }

    class Factory(
        private val clientModule: ClientModule
    ) {
        fun create(): UserDataManager {
            return UserDataManager(
                userModule = clientModule.userModule,
                sdkDispatchers = clientModule.clientOptions.sdkDispatchers
            )
        }
    }
}