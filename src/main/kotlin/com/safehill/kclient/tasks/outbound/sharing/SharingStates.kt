package com.safehill.kclient.tasks.outbound.sharing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class SharingStates {
    private val _sharingItems = MutableStateFlow<Map<String, SharingItem>>(emptyMap())
    val sharingItems = _sharingItems.asStateFlow().map { it.values.toList() }

    val statesByGlobalId = _sharingItems.map { items ->
        items.values.associate { it.request.globalIdentifier to it.state }
    }

    val statesByLocalId = _sharingItems.map { items ->
        items.values.associate { it.request.localIdentifier to it.state }
    }

    fun updateState(sharingId: String, state: SharingState) {
        _sharingItems.update { items ->
            val existingItem = items[sharingId]
            if (existingItem != null) {
                items + (sharingId to existingItem.withState(state))
            } else {
                items
            }
        }
    }

    fun addItem(request: SharingRequest, initialState: SharingState = SharingState.Pending) {
        val newItem =
            SharingItem(request = request, state = SharingState.Pending).withState(initialState)
        _sharingItems.update { items ->
            items + (newItem.id to newItem)
        }
    }

    fun removeItem(sharingId: String) {
        _sharingItems.update { items ->
            items - sharingId
        }
    }

    fun getItemByGlobalId(globalIdentifier: String): SharingItem? {
        return _sharingItems.value.values.find { it.request.globalIdentifier == globalIdentifier }
    }

    fun getItemByLocalId(localIdentifier: String): SharingItem? {
        return _sharingItems.value.values.find { it.request.localIdentifier == localIdentifier }
    }
}