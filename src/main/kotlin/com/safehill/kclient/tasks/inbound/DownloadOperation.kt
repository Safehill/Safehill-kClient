package com.safehill.kclient.tasks.inbound

import com.safehill.SafehillClient
import com.safehill.kclient.network.api.UserFlow


interface DownloadOperation {

    val listeners: List<DownloadOperationListener>

    val safehillClient: SafehillClient

    val userFlow: UserFlow
        get() = safehillClient.userFlow

}