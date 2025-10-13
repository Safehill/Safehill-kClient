package com.safehill.kclient.network.remote

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AssetVersionOutputDTO
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.getOrThrow
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object S3Proxy {
    suspend fun BaseApi.upload(
        data: ByteArray,
        url: String
    ) {
        client
            .put {
                url(urlString = url)
                setBody(data)
            }
            .getOrThrow<ByteArray>()
    }

    suspend fun BaseApi.fetchAssets(serverAssets: List<AssetOutputDTO>): Map<AssetGlobalIdentifier, EncryptedAsset> {
        return withContext(Dispatchers.IO) {
            val deferredResults = coroutineScope {
                serverAssets.map { serverAsset ->
                    async {
                        fetchAsset(serverAsset)
                    }
                }
            }

            deferredResults
                .awaitAll()
                .associateBy { it.globalIdentifier }
        }
    }

    private suspend fun BaseApi.fetchAsset(
        asset: AssetOutputDTO
    ): EncryptedAsset {
        val deferredResults = coroutineScope {
            asset.versions.map { serverAssetVersion ->
                async {
                    fetchAssetVersion(serverAssetVersion)
                }
            }
        }

        val encryptedVersions = deferredResults
            .awaitAll()


        return EncryptedAsset(
            globalIdentifier = asset.globalIdentifier,
            localIdentifier = asset.localIdentifier,
            creationDate = asset.creationDate,
            encryptedVersions = encryptedVersions.associateBy { version -> version.quality },
            fingerPrint = null
        )
    }

    private suspend fun BaseApi.fetchAssetVersion(assetVersion: AssetVersionOutputDTO): EncryptedAssetVersion {
        return EncryptedAssetVersion(
            quality = AssetQuality.entries.first { it.versionName == assetVersion.versionName },
            encryptedData = fetchData(assetVersion.presignedURL),
            encryptedSecret = assetVersion.encryptedSecret,
            publicKeyData = assetVersion.publicKeyData,
            publicSignatureData = assetVersion.publicSignatureData
        )

    }

    private suspend fun BaseApi.fetchData(preSignedURL: String): ByteArray {
        return client.get(preSignedURL).getOrThrow<ByteArray>()
    }
}