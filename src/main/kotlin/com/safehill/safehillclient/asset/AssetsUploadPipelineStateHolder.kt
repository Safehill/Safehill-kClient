package com.safehill.safehillclient.asset

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.tasks.outbound.UploadOperationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssetsUploadPipelineStateHolder : UploadOperationListener {

    private val _uploadingAssets: MutableStateFlow<Map<AssetLocalIdentifier, UploadPipelineState>> =
        MutableStateFlow(mapOf())
    val uploadingAssets = _uploadingAssets.asStateFlow()

    private fun updateAssetState(
        localIdentifier: AssetLocalIdentifier,
        state: UploadPipelineState
    ) {
        val updatedMap = _uploadingAssets.value.toMutableMap()
            .apply { this[localIdentifier] = state }
        _uploadingAssets.value = updatedMap
    }

    override fun startedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId
    ) {
        updateAssetState(localIdentifier, UploadPipelineState.Encrypting)
    }

    override fun finishedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
        updateAssetState(localIdentifier, UploadPipelineState.Encrypted)
    }

    override fun failedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId
    ) {
        updateAssetState(localIdentifier, UploadPipelineState.FailedEncrypting)
    }

    override fun startedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId
    ) {
        updateAssetState(localIdentifier, UploadPipelineState.Uploading)
    }

    override fun finishedUploading(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
    ) {
        updateAssetState(localIdentifier, UploadPipelineState.Uploaded)
    }

    override fun failedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
        updateAssetState(localIdentifier, UploadPipelineState.FailedUploading)
    }

    override fun startedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
        val updatedMap = _uploadingAssets.value.toMutableMap()
            .apply { this[globalIdentifier] = UploadPipelineState.Sharing(users) }
        _uploadingAssets.value = updatedMap
    }

    override fun finishedSharing(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
        val updatedMap = _uploadingAssets.value.toMutableMap()
            .apply { this[globalIdentifier] = UploadPipelineState.Shared(users) }
        _uploadingAssets.value = updatedMap
    }

    override fun failedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
        val updatedMap = _uploadingAssets.value.toMutableMap()
            .apply { this[globalIdentifier] = UploadPipelineState.FailedSharing(users) }
        _uploadingAssets.value = updatedMap
    }
}
