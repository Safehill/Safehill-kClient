package com.safehill.kclient.models.assets

import java.net.URI
import java.util.Date

sealed class Asset {
    data class FromAndroidPhotosLibrary(val androidAsset: AndroidAsset) : Asset()
    data class FromAndroidPhotosLibraryBackedUp(val backedUpAndroidAsset: BackedUpAndroidAsset) :
        Asset()

    data class Downloading(val assetDescriptor: AssetDescriptor) : Asset()
    data class Downloaded(val decryptedAsset: DecryptedAsset) : Asset()

    val debugType: String
        get() = when (this) {
            is FromAndroidPhotosLibrary -> "fromAndroidPhotosLibrary"
            is FromAndroidPhotosLibraryBackedUp -> "fromAndroidPhotosLibraryBackedUp"
            is Downloading -> "downloading"
            is Downloaded -> "downloaded"
        }

    val identifier: String
        get() = when (this) {
            is FromAndroidPhotosLibrary -> androidAsset.localIdentifier
            is FromAndroidPhotosLibraryBackedUp -> backedUpAndroidAsset.androidAsset.localIdentifier
            is Downloading -> assetDescriptor.localIdentifier ?: assetDescriptor.globalIdentifier
            is Downloaded -> decryptedAsset.localIdentifier ?: decryptedAsset.globalIdentifier
        }

    val localIdentifier: String?
        get() = when (this) {
            is FromAndroidPhotosLibrary -> androidAsset.localIdentifier
            is FromAndroidPhotosLibraryBackedUp -> backedUpAndroidAsset.androidAsset.localIdentifier
            is Downloading -> assetDescriptor.localIdentifier
            is Downloaded -> decryptedAsset.localIdentifier
        }

    val globalIdentifier: String?
        get() = when (this) {
            is FromAndroidPhotosLibrary -> null
            is FromAndroidPhotosLibraryBackedUp -> backedUpAndroidAsset.globalIdentifier
            is Downloading -> assetDescriptor.globalIdentifier
            is Downloaded -> decryptedAsset.globalIdentifier
        }

    val creationDate: Date?
        get() = when (this) {
            is FromAndroidPhotosLibrary -> androidAsset.creationDate
            is FromAndroidPhotosLibraryBackedUp -> backedUpAndroidAsset.androidAsset.creationDate
            is Downloading -> assetDescriptor.creationDate
            is Downloaded -> decryptedAsset.creationDate
        }

    val isFromLocalLibrary: Boolean
        get() = when (this) {
            is FromAndroidPhotosLibrary, is FromAndroidPhotosLibraryBackedUp -> true
            else -> false
        }

    val isDownloading: Boolean
        get() = this is Downloading

    val isFromRemoteLibrary: Boolean
        get() = when (this) {
            is Downloaded, is FromAndroidPhotosLibraryBackedUp, is Downloading -> true
            else -> false
        }

    val uploadState: AssetDescriptor.UploadState
        get() {
//            val auc = AssetsUploadController.shared
//            return localIdentifier?.let { auc.uploadState(it) }
//                ?: globalIdentifier?.let { auc.uploadState(it) }
//                ?: AssetDescriptor.UploadState.NotStarted
            return AssetDescriptor.UploadState.NotStarted
        }

    val width: Int?
        get() = when (this) {
            is FromAndroidPhotosLibrary -> androidAsset.pixelWidth
            is FromAndroidPhotosLibraryBackedUp -> backedUpAndroidAsset.androidAsset.pixelWidth
            else -> null
        }

    val height: Int?
        get() = when (this) {
            is FromAndroidPhotosLibrary -> androidAsset.pixelHeight
            is FromAndroidPhotosLibraryBackedUp -> backedUpAndroidAsset.androidAsset.pixelHeight
            else -> null
        }
}

data class AndroidAsset(
    val localIdentifier: String,
    val creationDate: Date?,
    val pixelWidth: Int?,
    val pixelHeight: Int?,
    val uri: URI?,
    val displayName: String?
)

data class BackedUpAndroidAsset(
    val globalIdentifier: String?,
    val androidAsset: AndroidAsset
)
