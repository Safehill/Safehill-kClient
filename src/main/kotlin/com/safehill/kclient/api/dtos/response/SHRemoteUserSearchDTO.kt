package com.safehill.kclient.api.dtos.response

import com.safehill.kclient.models.users.RemoteUser
import kotlinx.serialization.Serializable

@Serializable
data class SHRemoteUserSearchDTO(
    val items: List<RemoteUser>,
    val metadata: PaginationMetadataDTO
)