package com.safehill.kclient.network.api.asset

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.AssetOutputDTO

interface AssetApi {


    /**
     * Create encrypted assets and their versions on the server, and upload encrypted asset versions data to the CDN
     *  @param assets: the encrypted data for each asset
     *  @param groupId: the group identifier used for the first upload
     *  @return
     * the list of assets created*/
    suspend fun upload(assets: List<EncryptedAsset>, groupId: GroupId)

    /**
     * Retrieves encrypted data for the asset.
     *
     * @param globalIdentifiers Optional filtering by global identifiers.
     * @param versions Optional filtering by version.
     * @return The encrypted assets retrieved from the server.
     */
    suspend fun getEncryptedAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>
    ): Map<AssetGlobalIdentifier, EncryptedAsset>

    /**
     * Retrieves asset,its metadata along with preSignedUrl from the server.
     *
     * @param globalIdentifiers A list of asset global identifiers used to filter the assets.
     * @param versions A list of asset qualities or versions to filter the results.
     * @return A list of [AssetOutputDTO] containing the asset data and associated metadata.
     */
    suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>
    ): List<AssetOutputDTO>

}
