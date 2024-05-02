package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.tasks.BackgroundOperationProcessor

class FullUploadPipelineProcessor private constructor(delayedStartInSeconds: Int, dispatchIntervalInSeconds: Int) :
    BackgroundOperationProcessor<FullUploadPipelineOperation>(delayedStartInSeconds, dispatchIntervalInSeconds) {
    companion object {
        val shared = FullUploadPipelineProcessor(1, 7)
    }
}
