package com.safehill.kclient.models.assets

import java.util.Date

class AssetDescriptorImpl(
    override val globalIdentifier: String,
    override val localIdentifier: String?,
    override val creationDate: Date?,
    override var uploadState: AssetDescriptor.UploadState,
    override var sharingInfo: AssetDescriptor.SharingInfo
) : AssetDescriptor {

    class SharingInfoImpl(
        override val sharedByUserIdentifier: String,
        override val sharedWithUserIdentifiersInGroup: Map<String, String>,
        override val groupInfoById: Map<String, AssetDescriptor.SharingInfo.GroupInfo>
    ) : AssetDescriptor.SharingInfo {

        class GroupInfoImpl(
            override val name: String?, override val createdAt: Date?
        ) : AssetDescriptor.SharingInfo.GroupInfo {}
    }
}

