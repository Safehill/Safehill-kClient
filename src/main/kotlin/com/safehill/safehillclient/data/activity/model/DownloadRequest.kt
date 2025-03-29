package com.safehill.safehillclient.data.activity.model

import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.safehillclient.data.user.model.AppUser
import java.time.Instant


class DownloadRequest(
    assetIdentifiers: List<GlobalIdentifier>,
    groupId: String,
    eventOriginator: AppUser,
    shareInfo: Map<AppUser, Instant>,
    createdAt: Instant
) : AbstractAssetActivity(
    assetIdentifiers = assetIdentifiers,
    groupId = groupId,
    eventOriginator = eventOriginator,
    shareInfo = shareInfo,
    createdAt = createdAt
)

