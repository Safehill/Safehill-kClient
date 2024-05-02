package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.controllers.PhotosIndexer
import com.safehill.kclient.models.CachingImageManager
import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.user.AuthenticatedLocalUser
import com.safehill.kclient.tasks.AbstractBackgroundOperation
import com.safehill.kclient.tasks.BackgroundOperation
import com.safehill.kclient.tasks.OutboundAssetOperationDelegate
import java.util.function.Consumer
import java.util.logging.Logger


class FullUploadPipelineOperation(
    user: AuthenticatedLocalUser,
    assetsDelegates: List<OutboundAssetOperationDelegate>,
    parallelization: ParallelizationOption,
    imageManager: CachingImageManager?,
    photoIndexer: PhotosIndexer
) : AbstractBackgroundOperation() {
    enum class ParallelizationOption {
        AGGRESSIVE,
        CONSERVATIVE
    }

    private val log: Logger = Logger.getLogger("com.gf.safehill.fupo")
    private val user: AuthenticatedLocalUser
    private val assetsDelegates: List<OutboundAssetOperationDelegate>
    private val imageManager: CachingImageManager?
    private val photoIndexer: PhotosIndexer
    private val parallelization: ParallelizationOption

    init {
        this.user = user
        this.assetsDelegates = assetsDelegates
        this.parallelization = parallelization
        this.imageManager = imageManager ?: CachingImageManager()
        this.photoIndexer = photoIndexer
    }

    override fun clone(): BackgroundOperation {
        return FullUploadPipelineOperation(
            user,
            assetsDelegates,
            parallelization,
            imageManager,
            photoIndexer
        )
    }

    fun run(
        localIdentifiers: List<String>,
        groupId: String,
        sharedWith: List<SHServerUser>,
        completionHandler: Consumer<Result<Unit>>
    ) {
        // Implement run method
    }

    override fun run(completionHandler: Consumer<Result<Unit>>) {
        runFetchCycle { resultFetch ->
            if (resultFetch.isFailure) {
                log.severe("error running FETCH step: " +
                    resultFetch.exceptionOrNull()?.localizedMessage
                )
                completionHandler.accept(resultFetch.exceptionOrNull()!!.let { Result.failure(it) })
            } else {
                // TODO: handle cancelled
                runEncryptionCycle { resultEncrypt ->
                    if (resultEncrypt.isFailure) {
                        log.severe(
                            "error running ENCRYPT step: " + resultFetch.exceptionOrNull()?.localizedMessage
                        )
                        completionHandler.accept(resultFetch.exceptionOrNull()!!.let { Result.failure(it) })
                    } else {
                        // TODO: handle cancelled
                        runUploadCycle { resultUpload ->
                            if (resultUpload.isFailure) {
                                log.severe(
                                    "error running UPLOAD step: " + resultFetch.exceptionOrNull()?.localizedMessage
                                )
                                completionHandler.accept(resultFetch.exceptionOrNull()!!.let { Result.failure(it) })
                            } else {
                                // TODO: handle cancelled
                                runFetchCycle { resultFetchAgain ->
                                    if (resultFetchAgain.isFailure) {
                                        log.severe(
                                            "error running step FETCH': " + resultFetch.exceptionOrNull()?.localizedMessage
                                        )
                                        completionHandler.accept(resultFetch.exceptionOrNull()!!.let { Result.failure(it) })
                                    } else {
                                        // TODO: handle cancelled
                                        runShareCycle(completionHandler)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun runFetchCycle(completionHandler: Consumer<Result<Unit>>) {
        log.info("Running fetch cycle")
        completionHandler.accept(Result.success(Unit))


    }

    private fun runEncryptionCycle(completionHandler: Consumer<Result<Unit>>) {
        log.info("Running encryption cycle")
        completionHandler.accept(Result.success(Unit))
    }

    private fun runUploadCycle(completionHandler: Consumer<Result<Unit>>) {
        log.info("Running upload cycle")
        completionHandler.accept(Result.success(Unit))
    }

    private fun runShareCycle(completionHandler: Consumer<Result<Unit>>) {
        log.info("Running share cycle")
        completionHandler.accept(Result.success(Unit))
    }
}

