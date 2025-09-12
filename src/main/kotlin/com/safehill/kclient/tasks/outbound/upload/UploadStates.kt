package com.safehill.kclient.tasks.outbound.upload

import com.safehill.kclient.tasks.outbound.model.UploadItem
import com.safehill.kclient.tasks.outbound.model.UploadRequest
import com.safehill.kclient.tasks.outbound.model.UploadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class UploadStates {
    private val _uploadItems = MutableStateFlow<Map<String, UploadItem>>(emptyMap())
    val uploadItems = _uploadItems.asStateFlow().map { it.values.toList() }

    val statesByGlobalId = _uploadItems.map { items ->
        items.values.associate { it.request.globalIdentifier to it.state }
    }

    val statesByLocalId = _uploadItems.map { items ->
        items.values.associate { it.request.localIdentifier to it.state }
    }

    fun updateState(uploadId: String, state: UploadState) {
        _uploadItems.update { items ->
            val existingItem = items[uploadId]
            if (existingItem != null) {
                items + (uploadId to existingItem.withState(state))
            } else {
                items
            }
        }
    }

    fun addItem(request: UploadRequest, initialState: UploadState = UploadState.Pending) {
        val newItem = UploadItem(request = request, state = initialState)
        _uploadItems.update { items ->
            items + (newItem.id to newItem)
        }
    }

    fun removeItem(uploadId: String) {
        _uploadItems.update { items ->
            items - uploadId
        }
    }

    fun getItemByGlobalId(globalIdentifier: String): UploadItem? {
        return _uploadItems.value.values.find { it.request.globalIdentifier == globalIdentifier }
    }

    fun getItemByLocalId(localIdentifier: String): UploadItem? {
        return _uploadItems.value.values.find { it.request.localIdentifier == localIdentifier }
    }
}