package com.safehill.kclient.network

import com.safehill.kclient.api.SafehillApi
import com.safehill.kclient.models.SHServerUser

interface ServerProxyInterface : SafehillApi {
    suspend fun getAllLocalUsers(): List<SHServerUser>
}

