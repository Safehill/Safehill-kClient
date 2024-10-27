package com.safehill.kclient.network.local

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO

interface LocalAssetApi {

    suspend fun storeAssetsWithDescriptor(encryptedAssetsWithDescriptor: Map<AssetDescriptor, EncryptedAsset>)

    suspend fun storeThreadAssets(
        threadId: String, conversationThreadAssetsDTO: ConversationThreadAssetsDTO
    )
}