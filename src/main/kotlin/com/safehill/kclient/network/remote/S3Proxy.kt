package com.safehill.kclient.network.remote

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AssetVersionOutputDTO
import com.safehill.kclient.network.api.getOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class S3Proxy {

    companion object {

        suspend fun uploadData(dataByPresignedURL: Map<String, ByteArray>) {
            val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)
            val deferredResults = dataByPresignedURL.map { kv ->
                coroutineScope.async {
                    upload(kv.value, kv.key)
                }
            }
            deferredResults
                .awaitAll()
        }

        suspend fun upload(
            data: ByteArray,
            url: String
        ) {
            url.httpPut()
                .response()
                .getOrThrow()
        }

        suspend fun fetchAssets(serverAssets: List<AssetOutputDTO>): Map<AssetGlobalIdentifier, EncryptedAsset> {
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

        private suspend fun fetchAsset(
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
                asset.globalIdentifier,
                asset.localIdentifier,
                asset.creationDate,
                encryptedVersions.associateBy { version -> version.quality }
            )
        }

        private fun fetchAssetVersion(assetVersion: AssetVersionOutputDTO): EncryptedAssetVersion {
            return EncryptedAssetVersion(
                quality = AssetQuality.entries.first { it.value == assetVersion.versionName },
                encryptedData = fetchData(assetVersion.presignedURL),
                encryptedSecret = assetVersion.encryptedSecret,
                publicKeyData = assetVersion.publicKeyData,
                publicSignatureData = assetVersion.publicSignatureData
            )

        }

        private fun fetchData(preSignedURL: String): ByteArray {
            return preSignedURL.httpGet().response().getOrThrow()
        }

    }

}
