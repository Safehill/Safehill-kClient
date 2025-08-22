package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.EnumIntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class AssetShareDTO(
    val globalAssetIdentifier: String,
    val versionSharingDetails: List<ShareVersionDetails>,
    val groupId: String,
    val asPhotoMessageInThreadId: String?,
    val sharingOption: SharingOption?
)


@Serializable(with = SharingOption.Companion::class)
enum class SharingOption(
    val serverValue: Int
) {
    Confidential(0),
    Shared(1);

    companion object : EnumIntSerializer<SharingOption>() {
        override fun codeSelector(item: SharingOption): Int {
            return item.serverValue
        }

        override fun fromCode(int: Int): SharingOption {
            return SharingOption.entries.first { it.serverValue == int }
        }
    }
}