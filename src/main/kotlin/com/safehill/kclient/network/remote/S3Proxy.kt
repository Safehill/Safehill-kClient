package com.safehill.kclient.network.remote

import com.github.kittinunf.fuel.httpGet
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AssetVersionOutputDTO
import com.safehill.kclient.network.api.getOrThrow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class S3Proxy {

    companion object {
        suspend fun fetchAssets(serverAssets: List<AssetOutputDTO>): Map<AssetGlobalIdentifier, EncryptedAsset> {
            val deferredResults = coroutineScope {
                serverAssets.map { serverAsset ->
                    async { fetchAsset(serverAsset) }
                }
            }

            return deferredResults
                .awaitAll()
                .filterNotNull()
                .associateBy { it.globalIdentifier }
        }

        private suspend fun fetchAsset(
            asset: AssetOutputDTO
        ): EncryptedAsset? {
            val deferredResults = coroutineScope {
                asset.versions.map { serverAssetVersion ->
                    async {
                        try {
                            fetchAssetVersion(serverAssetVersion)
                        } catch (e: Exception) {
                            println(e)
                            // TODO: Propagate errors instead of just swallowing them
                            null
                        }
                    }
                }
            }

            val encryptedVersions = deferredResults
                .awaitAll()
                .filterNotNull()

            if (encryptedVersions.isEmpty()) {
                return null
            }

            return EncryptedAsset(
                asset.globalIdentifier,
                asset.localIdentifier,
                asset.creationDate,
                encryptedVersions.associateBy { version -> version.quality }
            )
        }

        private fun fetchAssetVersion(assetVersion: AssetVersionOutputDTO): EncryptedAssetVersion? {
            assetVersion.presignedURL?.let { url ->
                return EncryptedAssetVersion(
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

        private fun fetchData(presignedURL: String): ByteArray {
            return presignedURL.httpGet().response().getOrThrow()
        }

    }

}
