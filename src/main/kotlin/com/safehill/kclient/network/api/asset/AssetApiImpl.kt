package com.safehill.kclient.network.api.asset

import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AssetSearchCriteriaDTO
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.postRequestForResponse
import com.safehill.kclient.network.remote.S3Proxy.fetchAssets

class AssetApiImpl(
    baseApi: BaseApi,
    private val safehillLogger: SafehillLogger
) : AssetApi, BaseApi by baseApi {

    private val assetUploader = AssetUploader(
        baseApi = baseApi,
        safehillLogger = safehillLogger
    )

    override suspend fun upload(
        assets: List<EncryptedAsset>,
        groupId: GroupId
    ) {
        assetUploader.upload(
            assets = assets,
            groupId = groupId
        )
    }

    @Throws
    override suspend fun getEncryptedAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>,
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        val assetOutputDTOs = getAssets(
            globalIdentifiers = globalIdentifiers,
            versions = versions
        )
        return fetchAssets(assetOutputDTOs)
    }

    override suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>
    ): List<AssetOutputDTO> {
        val assetFilterCriteriaDTO = AssetSearchCriteriaDTO(
            globalIdentifiers = globalIdentifiers,
            versionNames = versions.map { it.versionName }
        )

        return postRequestForResponse<AssetSearchCriteriaDTO, List<AssetOutputDTO>>(
            endPoint = "/assets/retrieve",
            request = assetFilterCriteriaDTO
        )
    }
}