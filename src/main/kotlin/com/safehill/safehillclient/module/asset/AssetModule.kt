package com.safehill.safehillclient.module.asset

import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.tasks.outbound.AssetEncrypter
import com.safehill.safehillclient.asset.AssetsUploadPipelineStateHolder
import com.safehill.safehillclient.asset.OutboundAssetsState
import com.safehill.safehillclient.module.config.ClientOptions
import com.safehill.safehillclient.module.platform.PlatformModule

class AssetModule(
    private val platformModule: PlatformModule,
    private val clientOptions: ClientOptions,
    private val postAssetEmbeddings: Boolean
) {

    @Deprecated("Use outboundAssetsState instead")
    val assetsUploadPipelineStateHolder: AssetsUploadPipelineStateHolder by lazy {
        AssetsUploadPipelineStateHolder()
    }

    val outboundAssetsState by lazy {
        OutboundAssetsState()
    }
    val assetEncrypter by lazy {
        AssetEncrypter(
            resizer = platformModule.imageResizer,
            localAssetGetter = platformModule.localAssetGetter,
            assetEmbeddings = platformModule.assetEmbeddings,
            safehillLogger = clientOptions.safehillLogger,
            postAssetEmbeddings = postAssetEmbeddings
        )
    }

    val assetDescriptorCache by lazy {
        AssetDescriptorsCache(
            clientScope = clientOptions.clientScope
        )
    }

}