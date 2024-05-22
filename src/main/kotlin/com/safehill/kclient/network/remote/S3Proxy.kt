package com.safehill.kclient.network.remote

import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetImpl
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.assets.EncryptedAssetVersionImpl
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AssetVersionOutputDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class S3Proxy {

    companion object {
        suspend fun fetchAssets(serverAssets: List<AssetOutputDTO>): Map<AssetGlobalIdentifier, EncryptedAsset> {
            val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)
            val deferredResults = serverAssets.map { serverAsset ->
                coroutineScope.async {
                    fetchAsset(serverAsset, this)
                }
            }

            return deferredResults
                .awaitAll()
                .filterNotNull()
                .associateBy { it.globalIdentifier }
        }
        private suspend fun fetchAsset(
            asset: AssetOutputDTO,
            coroutineScope: CoroutineScope
        ): EncryptedAsset? {
            val deferredResults = asset.versions.map { serverAssetVersion ->
                coroutineScope.async {
                    try {
                        fetchAssetVersion(serverAssetVersion)
                    } catch (e: Exception) {
                        println(e)
                        // TODO: Propagate errors instead of just swallowing them
                        null
                    }
                }
            }

            val encryptedVersions = deferredResults
                .awaitAll()
                .filterNotNull()

            if (encryptedVersions.isEmpty()) {
                return null
            }

            return EncryptedAssetImpl(
                asset.globalIdentifier,
                asset.localIdentifier,
                asset.creationDate,
                encryptedVersions.associateBy { version -> version.quality }
            )
        }

        private suspend fun fetchAssetVersion(assetVersion: AssetVersionOutputDTO): EncryptedAssetVersion? {
            assetVersion.presignedURL?.let { url ->
                return EncryptedAssetVersionImpl(
                    quality = AssetQuality.entries.first { it.value == assetVersion.versionName },
                    encryptedData = fetchData(url),
                    encryptedSecret = assetVersion.encryptedSecret,
                    publicKeyData = assetVersion.publicKeyData,
                    publicSignatureData = assetVersion.publicSignatureData
                )
            } ?: run {
                return null
            }
        }

        private suspend fun fetchData(presignedURL: String): ByteArray {
            return presignedURL.httpGet().response().getOrThrow()
        }

        private fun <T> ResponseResultOf<T>.getOrThrow(): T {
            return when (val result = this.third) {
                is Result.Success -> result.value
                is Result.Failure -> throw result.error
            }
        }
    }

}
