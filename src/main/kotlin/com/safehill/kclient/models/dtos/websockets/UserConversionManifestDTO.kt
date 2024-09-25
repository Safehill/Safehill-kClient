package com.safehill.kclient.models.dtos.websockets

import com.safehill.kclient.models.users.RemoteUser
import kotlinx.serialization.Serializable

@Serializable
data class UserConversionManifestDTO(
    val assetIdsByGroupId: AssetIdsByGroupId,
    val newUser: RemoteUser,
    val threadIds: List<String>
) : WebSocketMessage


//todo figure out the type of asset id by group id when dealing with asset and groups.
@Serializable

class AssetIdsByGroupId