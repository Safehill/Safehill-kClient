package com.safehill.kclient.models

import java.util.Date

class SHAssetDescriptorImpl(
    override val globalIdentifier: String,
    override val localIdentifier: String?,
    override val creationDate: Date?,
    override var uploadState: SHAssetDescriptor.UploadState,
    override var sharingInfo: SHAssetDescriptor.SharingInfo
) : SHAssetDescriptor {

    class SharingInfoImpl(
        override val sharedByUserIdentifier: String,
        override val sharedWithUserIdentifiersInGroup: Map<String, String>,
        override val groupInfoById: Map<String, SHAssetDescriptor.SharingInfo.GroupInfo>
    ) : SHAssetDescriptor.SharingInfo {

        class GroupInfoImpl(
            override val name: String?, override val createdAt: Date?
        ) : SHAssetDescriptor.SharingInfo.GroupInfo {}
    }
}

