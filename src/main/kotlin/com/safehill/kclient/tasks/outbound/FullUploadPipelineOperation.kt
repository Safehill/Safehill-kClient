package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.user.AuthenticatedLocalUser
import com.safehill.kclient.tasks.AbstractBackgroundOperation
import com.safehill.kclient.tasks.BackgroundOperation
import java.util.logging.Logger


class FullUploadPipelineOperation(
    user: AuthenticatedLocalUser,
    assetsDelegates: List<OutboundAssetOperationDelegate>,
    threadsDelegates: List<ThreadSyncingDelegate>,
    parallelization: ParallelizationOption,
    imageManager: CachingImageManager?,
    photoIndexer: PhotosIndexer
) : AbstractBackgroundOperation() {
    enum class ParallelizationOption {
        AGGRESSIVE,
        CONSERVATIVE
    }

    private val log: Logger = Logger.getLogger("com.gf.safehill.BG")
    private val user: AuthenticatedLocalUser
    private val assetsDelegates: List<OutboundAssetOperationDelegate>
    private val threadsDelegates: List<ThreadSyncingDelegate>
    private val imageManager: CachingImageManager?
    private val photoIndexer: PhotosIndexer
    private val parallelization: ParallelizationOption

    init {
        this.user = user
        this.assetsDelegates = assetsDelegates
        this.threadsDelegates = threadsDelegates
        this.parallelization = parallelization
        this.imageManager = if (imageManager != null) imageManager else CachingImageManager()
        this.photoIndexer = photoIndexer
    }

    override fun clone(): BackgroundOperation {
        return FullUploadPipelineOperation(
            user,
            assetsDelegates,
            threadsDelegates,
            parallelization,
            imageManager,
            photoIndexer
        )
    }

    fun run(
        localIdentifiers: List<String?>?,
        groupId: String?,
        sharedWith: List<SHServerUser?>?,
        completionHandler: ResultHandler<Void?, Error?>?
    ) {
        // Implement run method
    }

    fun run(completionHandler: ResultHandler<Void?, Error?>?) {
        // TODO
    }

    private fun runFetchCycle(completionHandler: ResultHandler<Void, Error>) {
        // TODO
    }

    private fun runEncryptionCycle(completionHandler: ResultHandler<Void, Error>) {
        // TODO
    }

    private fun runUploadCycle(completionHandler: ResultHandler<Void, Error>) {
        // TODO
    }

    private fun runShareCycle(completionHandler: ResultHandler<Void, Error>) {
        // TODO
    }
}

