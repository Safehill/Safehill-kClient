package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.UserIdentifier
import java.time.Instant

data class AssetDescriptorImpl(
    override val globalIdentifier: AssetGlobalIdentifier,
    override val localIdentifier: AssetLocalIdentifier,
    override val creationDate: Instant,
    override var uploadState: UploadState,
    override var sharingInfo: SharingInfo
) : AssetDescriptor {

    data class SharingInfoImpl(
        override val sharedByUserIdentifier: UserIdentifier,
        override val groupIdsByRecipientUserIdentifier: Map<UserIdentifier, List<GroupId>>,
        override val groupInfoById: Map<GroupId, GroupInfo>
    ) : SharingInfo {

        data class GroupInfoImpl(
            override val name: String?, override val createdAt: Instant
        ) : GroupInfo
    }
}
