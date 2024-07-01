package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AssetInputDTO(
    val globalIdentifier: String,
    val localIdentifier: String?,
    // ISO8601 formatted datetime, representing the time the asset was created (i.e. the photo was taken).
    // Not to be confused with the time the asset was created on the server
    @Serializable(with = InstantSerializer::class) val creationDate: Instant,
    // On saving the asset this represents the groupId used by the sender at upload time
    // This groupId is stored in the AssetsVersionsUsers table in the rows representing the self-share (asset shared with self-user).
    // Sending this information in this payload avoids an extra unnecessary call to /share with sefl
    // If  no groupId was provided, it was a single-asset upload, so the identifier can be safely generated on server
    val groupId: String?,
    val versions: List<com.safehill.kclient.models.dtos.AssetVersionInputDTO>,
    // If set to true destroys all sharing information for the existing asset version, if one with the same name exists
    val forceUpdateVersions: Boolean? = false
)
