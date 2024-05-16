package com.safehill.kclient.models.assets

import java.net.URI
import java.util.Date

enum class Asset {
    FROM_ANDROID_LIBRARY {
        override fun debugType() = "fromApplePhotosLibrary"
        override fun identifier() = androidAsset.localIdentifier
        override fun localIdentifier() = androidAsset.localIdentifier
        override fun globalIdentifier() = null
        override fun creationDate() = androidAsset.creationDate
        override fun isFromLocalLibrary() = true
        override fun isDownloading() = false
        override fun isFromRemoteLibrary() = false
        override fun width() = androidAsset.pixelWidth
        override fun height() = androidAsset.pixelHeight
    },
    FROM_ANDROID_LIBRARY_BACKED_UP {
        override fun debugType() = "fromApplePhotosLibraryBackedUp"
        override fun identifier() = backedUpAndroidAsset.androidAsset.localIdentifier
        override fun localIdentifier() = backedUpAndroidAsset.androidAsset.localIdentifier
        override fun globalIdentifier() = backedUpAndroidAsset.androidAsset.globalIdentifier
        override fun creationDate() = backedUpAndroidAsset.androidAsset.creationDate
        override fun isFromLocalLibrary() = true
        override fun isDownloading() = false
        override fun isFromRemoteLibrary() = true
        override fun width() = backedUpAndroidAsset.androidAsset.pixelWidth
        override fun height() = backedUpAndroidAsset.androidAsset.pixelHeight
    },
    DOWNLOADING {
        override fun debugType() = "downloading"
        override fun identifier() = assetDescriptor.localIdentifier ?: assetDescriptor.globalIdentifier
        override fun localIdentifier() = assetDescriptor.localIdentifier
        override fun globalIdentifier() = assetDescriptor.globalIdentifier
        override fun creationDate() = assetDescriptor.creationDate
        override fun isFromLocalLibrary() = false
        override fun isDownloading() = true
        override fun isFromRemoteLibrary() = true
        override fun width() = null
        override fun height() = null
    },
    DOWNLOADED {
        override fun debugType() = "downloaded"
        override fun identifier() = decryptedAsset.localIdentifier ?: decryptedAsset.globalIdentifier
        override fun localIdentifier() = decryptedAsset.localIdentifier
        override fun globalIdentifier() = decryptedAsset.globalIdentifier
        override fun creationDate() = decryptedAsset.creationDate
        override fun isFromLocalLibrary() = false
        override fun isDownloading() = false
        override fun isFromRemoteLibrary() = true
        override fun width() = null
        override fun height() = null
    };

    abstract fun debugType(): String
    abstract fun identifier(): String
    abstract fun localIdentifier(): String?
    abstract fun globalIdentifier(): String?
    abstract fun creationDate(): Date?
    abstract fun isFromLocalLibrary(): Boolean
    abstract fun isDownloading(): Boolean
    abstract fun isFromRemoteLibrary(): Boolean
    abstract fun width(): Int?
    abstract fun height(): Int?


    lateinit var androidAsset: AndroidAsset
    lateinit var backedUpAndroidAsset: BackedUpAndroidAsset
    lateinit var assetDescriptor: AssetDescriptor
    lateinit var decryptedAsset: DecryptedAsset
}

data class AndroidAsset(
    val localIdentifier: String,
    val globalIdentifier: String?,
    val creationDate: Date?,
    val pixelWidth: Int?,
    val pixelHeight: Int?,
    val uri: URI?
)

data class BackedUpAndroidAsset(
    val androidAsset: AndroidAsset
)
