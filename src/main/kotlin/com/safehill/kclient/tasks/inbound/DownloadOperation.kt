package com.safehill.kclient.tasks.inbound


interface DownloadOperation {

    val listeners: List<DownloadOperationListener>

}