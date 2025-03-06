package com.safehill.safehillclient.module.asset

import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.tasks.outbound.AssetEncrypter
import com.safehill.safehillclient.asset.AssetsUploadPipelineStateHolder
import com.safehill.safehillclient.platform.PlatformModule
import com.safehill.safehillclient.platform.UserModule

class AssetModule(
    private val platformModule: PlatformModule,
    private val userModule: UserModule,
    private val userProvider: UserProvider
) {

    val assetsUploadPipelineStateHolder: AssetsUploadPipelineStateHolder by lazy {
        AssetsUploadPipelineStateHolder()
    }

    val assetEncrypter by lazy {
        AssetEncrypter(
            resizer = platformModule.imageResizer,
            userModule = userModule,
            userProvider = userProvider
        )
    }

    val assetDescriptorCache by lazy {
        AssetDescriptorsCache()
    }

}