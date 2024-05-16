package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.UserIdentifier
import java.util.Date

class AssetDescriptorImpl(
    override val globalIdentifier: AssetGlobalIdentifier,
    override val localIdentifier: AssetLocalIdentifier?,
    override val creationDate: Date?,
    override var uploadState: AssetDescriptor.UploadState,
    override var sharingInfo: AssetDescriptor.SharingInfo
) : AssetDescriptor {

    class SharingInfoImpl(
        override val sharedByUserIdentifier: UserIdentifier,
        override val sharedWithUserIdentifiersInGroup: Map<UserIdentifier, GroupId>,
        override val groupInfoById: Map<GroupId, AssetDescriptor.SharingInfo.GroupInfo>
    ) : AssetDescriptor.SharingInfo {

        class GroupInfoImpl(
            override val name: String?, override val createdAt: Date?
        ) : AssetDescriptor.SharingInfo.GroupInfo {}
    }
}
