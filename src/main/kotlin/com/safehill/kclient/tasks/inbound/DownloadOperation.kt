package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.users.LocalUser


interface DownloadOperation {

    val listeners: List<DownloadOperationListener>

    val user: LocalUser

}