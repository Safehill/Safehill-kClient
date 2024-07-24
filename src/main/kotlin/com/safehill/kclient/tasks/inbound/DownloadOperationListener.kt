package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.network.GlobalIdentifier


interface DownloadOperationListener {

    fun fetched(
        decryptedAsset: DecryptedAsset
    )

    fun didFailDownloadOfAsset(
        globalIdentifier: GlobalIdentifier,
        error: Throwable
    )

    fun didFailRepeatedlyDownloadOfAsset(
        globalIdentifier: GlobalIdentifier,
    )

}