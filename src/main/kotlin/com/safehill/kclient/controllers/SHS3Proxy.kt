package com.safehill.kclient.controllers

import com.safehill.kclient.api.dtos.SHAssetOutputDTO
import com.safehill.kclient.api.dtos.SHAssetVersionOutputDTO
import com.safehill.kclient.models.SHAssetQuality
import com.safehill.kclient.models.SHEncryptedAssetVersion
import com.safehill.kclient.models.SHEncryptedAssetVersionImpl

class SHS3Proxy(val presignedURL: String) {

    fun save(data: ByteArray, headers: Map<String, String> = emptyMap()) {}

    suspend fun retrieve(asset: SHAssetOutputDTO, version: SHAssetVersionOutputDTO, quality: SHAssetQuality): SHEncryptedAssetVersion {

        println("retrieving asset ${asset.globalIdentifier} version ${version.versionName} from S3 using ${this.presignedURL}")

        // TODO: Download data from CDN using presigned URL
        val dummyData = "".toByteArray()

        return SHEncryptedAssetVersionImpl(
            quality,
            dummyData,
            version.encryptedSecret,
            version.publicKeyData,
            version.publicSignatureData
        )
    }
}