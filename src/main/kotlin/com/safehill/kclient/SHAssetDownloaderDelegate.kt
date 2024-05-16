package com.safehill.kclient

import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHServerUser
import java.net.URI

interface SHAssetDownloaderDelegate {

    // The list of asset descriptors fetched from the server, filtering out what's already available locally
    // - descriptors: the descriptors
    // - users: the `SHServerUser` objects for user ids mentioned in the descriptors
    // - completionHandler: called when handling is complete
    fun didReceiveAssetDescriptors(
        descriptors: List<SHAssetDescriptor>,
        referencingUsers: Map<UserIdentifier, SHServerUser>,
        completionHandler: () -> Unit
    )

    // Notifies about assets in the local library that are linked to one on the server
    // - localToGlobal: The global identifier of the remote asset to the corresponding local `PHAsset` from the Apple Photos Library
    fun didIdentify(globalToLocalAssets: Map<GlobalIdentifier, ImageInfo>)
}

typealias GlobalIdentifier = String
typealias UserIdentifier = String

data class ImageInfo (
    val uri: URI?,
    val title: String?,
    val displayName: String?,
)
