package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.EnumIntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class AssetShareDTO(
    val globalAssetIdentifier: String,
    val versionSharingDetails: List<ShareVersionDetails>,
    val groupId: String,
    val asPhotoMessageInThreadId: String?,
    val sharingOption: AssetPermission?
)


@Serializable(with = AssetPermission.Companion::class)
enum class AssetPermission(
    val serverValue: Int
) {
    Confidential(0),
    Shared(1);

    companion object Companion : EnumIntSerializer<AssetPermission>() {
        override fun codeSelector(item: AssetPermission): Int {
            return item.serverValue
        }

        override fun fromCode(int: Int): AssetPermission {
            return AssetPermission.entries.first { it.serverValue == int }
        }
    }
}