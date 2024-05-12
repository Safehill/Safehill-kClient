package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.users.RemoteUser
import kotlinx.serialization.Serializable

@Serializable
data class RemoteUserSearchDTO(
    val items: List<RemoteUser>,
    val metadata: PaginationMetadataDTO
)