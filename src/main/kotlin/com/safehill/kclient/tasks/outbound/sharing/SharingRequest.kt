package com.safehill.kclient.tasks.outbound.sharing

import com.safehill.kclient.models.assets.AssetQuality
import java.util.UUID


data class SharingRequest(
    val id: String = UUID.randomUUID().toString(),
    val globalIdentifier: String,
    val localIdentifier: String,
    val qualities: List<AssetQuality>,
    val groupId: String,
    val recipients: List<String>,
    val threadId: String,
    val metadata: Map<String, String> = emptyMap()
)

data class SharingItem(
    val id: String = UUID.randomUUID().toString(),
    val request: SharingRequest,
    val state: SharingState,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun withState(newState: SharingState): SharingItem {
        return copy(state = newState, updatedAt = System.currentTimeMillis())
    }

    val isPending: Boolean get() = state is SharingState.Pending
    val isActive: Boolean get() = state is SharingState.Sharing
    val isCompleted: Boolean get() = state is SharingState.Success
    val isFailed: Boolean get() = state is SharingState.Failed
    val isCancelled: Boolean get() = state is SharingState.Cancelled
}