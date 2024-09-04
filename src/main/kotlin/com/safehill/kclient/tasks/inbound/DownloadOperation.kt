package com.safehill.kclient.tasks.inbound

import com.safehill.SafehillClient
import com.safehill.kclient.models.users.LocalUser


interface DownloadOperation {

    val listeners: List<DownloadOperationListener>


    val safehillClient: SafehillClient

    val user: LocalUser
        get() = safehillClient.currentUser

}