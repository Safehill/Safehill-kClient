package com.safehill.kclient.models.assets

import java.net.URI
import java.time.Instant

sealed class Asset {

    data class FromPhotosLibrary(
        val libraryPhoto: LibraryPhoto
    ) : Asset() {
        val localIdentifier = libraryPhoto.localIdentifier
    }

    data class BackedUpLibraryPhoto(
        val globalIdentifier: String,
        val libraryPhoto: LibraryPhoto
    ) : Asset()

    data class Downloading(
        val globalIdentifier: AssetGlobalIdentifier
    ) : Asset()

    data class Downloaded(val decryptedAsset: DecryptedAsset) : Asset()

    val debugType: String
        get() = when (this) {
            is FromPhotosLibrary -> "FromPhotosLibrary"
            is BackedUpLibraryPhoto -> "BackedUpLibraryPhoto"
            is Downloading -> "downloading"
            is Downloaded -> "downloaded"
        }

    val identifier: String
        get() = when (this) {
            is FromPhotosLibrary -> libraryPhoto.localIdentifier
            is BackedUpLibraryPhoto -> globalIdentifier
            is Downloading -> globalIdentifier
            is Downloaded -> decryptedAsset.globalIdentifier
        }

    val isFromLocalLibrary: Boolean
        get() = when (this) {
            is FromPhotosLibrary, is BackedUpLibraryPhoto -> true
            else -> false
        }

    val isDownloading: Boolean
        get() = this is Downloading

    val isFromRemoteLibrary: Boolean
        get() = when (this) {
            is Downloaded, is BackedUpLibraryPhoto, is Downloading -> true
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
            is FromPhotosLibrary -> libraryPhoto.pixelWidth
            is BackedUpLibraryPhoto -> libraryPhoto.pixelWidth
            else -> null
        }

    val height: Int?
        get() = when (this) {
            is FromPhotosLibrary -> libraryPhoto.pixelHeight
            is BackedUpLibraryPhoto -> libraryPhoto.pixelHeight
            else -> null
        }

    val uri: URI?
        get() = when (this) {
            is FromPhotosLibrary -> libraryPhoto.uri
            is BackedUpLibraryPhoto -> libraryPhoto.uri
            else -> null
        }

    val creationDate: Instant?
        get() = when (this) {
            is FromPhotosLibrary -> libraryPhoto.creationDate
            is BackedUpLibraryPhoto -> libraryPhoto.creationDate
            is Downloaded -> decryptedAsset.creationDate
            else -> null
        }

    val type: String
        get() = when (this) {
            is FromPhotosLibrary -> "from the Photos library"
            else -> "In your lockbox"
        }


}

data class LibraryPhoto(
    val localIdentifier: String,
    val creationDate: Instant?,
    val pixelWidth: Int?,
    val pixelHeight: Int?,
    val uri: URI?,
    val displayName: String?
)
