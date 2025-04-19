package com.safehill.kclient.network.api.asset

import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId

interface AssetApi {


    /**
     * Create encrypted assets and their versions on the server, and upload encrypted asset versions data to the CDN
     *  @param assets: the encrypted data for each asset
     *  @param groupId: the group identifier used for the first upload
     *  @return
     * the list of assets created*/
    suspend fun upload(assets: List<EncryptedAsset>, groupId: GroupId)
}
