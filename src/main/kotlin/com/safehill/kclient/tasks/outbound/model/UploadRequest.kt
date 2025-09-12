package com.safehill.kclient.tasks.outbound.model

import com.safehill.kclient.models.assets.AssetQuality
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Core upload models and interfaces
 */

data class UploadRequest(
    val id: String = UUID.randomUUID().toString(),
    val localIdentifier: String,
    val globalIdentifier: String,
    val qualities: List<AssetQuality>,
    val groupId: String,
    val recipients: List<String>,
    val threadId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)


data class UploadItem(
    val request: UploadRequest,
    val state: UploadState,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val id: String get() = request.id

    fun withState(newState: UploadState): UploadItem {
        return copy(state = newState, updatedAt = System.currentTimeMillis())
    }

    val isPending: Boolean get() = state is UploadState.Pending
    val isActive: Boolean get() = state is UploadState.InProgress
    val isCompleted: Boolean get() = state is UploadState.Success
    val isFailed: Boolean get() = state is UploadState.Failed
    val isCancelled: Boolean get() = state is UploadState.Cancelled
}

data class UploadStats(
    val total: Int,
    val pending: Int,
    val active: Int,
    val completed: Int,
    val failed: Int,
    val retrying: Int = 0
) {
    val inProgress: Int get() = active + retrying
    val finished: Int get() = completed + failed
}

interface UploadExecutor {
    suspend fun execute(request: UploadRequest): Flow<UploadState>
}

interface UploadStorage {
    suspend fun save(item: UploadItem)
    suspend fun load(): List<UploadItem>
    suspend fun remove(id: String)
    suspend fun clear()
    fun observe(): Flow<List<UploadItem>>
}

interface UploadManager {
    val items: Flow<List<UploadItem>>
    val stats: Flow<UploadStats>

    suspend fun enqueue(request: UploadRequest): Flow<UploadState>
    suspend fun retry(id: String): Flow<UploadState>?
    suspend fun cancel(id: String)
    suspend fun clearCompleted()
    suspend fun restorePending(): Flow<Pair<String, UploadState>>
}
