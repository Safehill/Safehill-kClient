package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.controllers.PhotosIndexer
import com.safehill.kclient.models.CachingImageManager
import com.safehill.kclient.models.user.AuthenticatedLocalUser
import com.safehill.kclient.tasks.OutboundAssetOperationDelegate
import org.junit.jupiter.api.Test

class FullUploadPipelineOperationTest {

    @Test
    fun testRun() {
        val operation = FullUploadPipelineOperation(
            user = AuthenticatedLocalUser(),
            assetsDelegates = arrayListOf(OutboundAssetOperationDelegate()),
            parallelization = FullUploadPipelineOperation.ParallelizationOption.AGGRESSIVE,
            imageManager = CachingImageManager(),
            photoIndexer = PhotosIndexer(),
        )

        operation.run()
    }
}
