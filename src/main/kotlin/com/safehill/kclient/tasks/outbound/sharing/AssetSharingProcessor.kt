package com.safehill.kclient.tasks.outbound.sharing

import com.safehill.kclient.tasks.upload.queue.ItemProcessor
import com.safehill.kclient.util.runCatchingSafe

class AssetSharingProcessor(
    private val sharingExecutor: SharingExecutor,
    private val sharingStates: SharingStates
) : ItemProcessor<SharingRequest> {

    override suspend fun onEnqueued(item: SharingRequest) {
        sharingStates.addItem(item, SharingState.Pending)
    }

    override suspend fun process(item: SharingRequest): Result<Unit> {
        return runCatchingSafe {
            sharingExecutor.execute(item).collect {
                sharingStates.updateState(item.id, it)
            }
        }
    }

}