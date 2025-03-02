package com.safehill.kclient.tasks.inbound

import com.safehill.SafehillClient


interface DownloadOperation {

    val listeners: List<DownloadOperationListener>

    val safehillClient: SafehillClient

}