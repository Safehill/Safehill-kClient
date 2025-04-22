package com.safehill.safehillclient.data.threads.model

import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.tasks.outbound.UploadFailure
import com.safehill.utils.flow.combineStates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

typealias ThreadId = String

class SharingAssetsState {

    private val threadToGroupMapping = MutableStateFlow<Map<ThreadId, List<GroupId>>>(mapOf())

    private val _assetsBeingShared = MutableStateFlow<Map<GroupId, SharingAssets>>(mapOf())

    val assetsBeingShared: StateFlow<Map<ThreadId, List<SharingAssets>>> = combineStates(
        _assetsBeingShared,
        threadToGroupMapping
    ) { assetsBeingShared, threadToGroupMap ->
        threadToGroupMap.mapValues { (threadId, groupIds) ->
            groupIds.mapNotNull { groupId ->
                assetsBeingShared[groupId]
            }
        }
    }

    fun removeSharingAssets(groupId: GroupId) {
        _assetsBeingShared.update { it - groupId }
    }

    fun setThreadToGroupMapping(threadId: ThreadId, groupId: GroupId) {
        threadToGroupMapping.update { initial ->
            val threadGroups = initial.getOrElse(threadId) { listOf() }
            initial + (threadId to (threadGroups + groupId).distinct())
        }
    }

    fun removeSharingAssets(
        groupId: GroupId,
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier
    ) {
        _assetsBeingShared.update { initial ->
            val sharingAssets = initial[groupId]
            if (sharingAssets == null) {
                initial
            } else {
                sharingAssets.removeSharingAsset(
                    globalIdentifier = globalIdentifier,
                    localIdentifier = localIdentifier
                ).takeIf { it.assets.isNotEmpty() }
                    ?.let { updatedSharingAssets -> initial + (groupId to updatedSharingAssets) }
                    ?: (initial - groupId)
            }
        }
    }

    fun setError(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        uploadFailure: UploadFailure
    ) {
        upsertSharingAssets(
            globalIdentifier = globalIdentifier,
            localIdentifier = localIdentifier,
            groupId = groupId,
            state = SharingAsset.State.Failed(uploadFailure)
        )
    }

    fun upsertSharingAssets(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: String,
        state: SharingAsset.State
    ) {
        _assetsBeingShared.update {
            val sharingAssets = it.getOrElse(groupId) {
                SharingAssets(
                    groupId = groupId,
                    assets = listOf(),
                    uploadedAt = Instant.now()
                )
            }
            val newEntry = groupId to sharingAssets.upsertSharingAsset(
                globalIdentifier = globalIdentifier,
                localIdentifier = localIdentifier,
                state = state,
                groupId = groupId
            )
            it + newEntry
        }
    }
}