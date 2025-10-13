package com.safehill.kclient.network.api.asset

import com.safehill.kclient.base64.base64EncodedString
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.Embeddings
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.AssetInputDTO
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AssetVersionInputDTO
import com.safehill.kclient.models.dtos.AssetVersionOutputDTO
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.postRequest
import com.safehill.kclient.network.api.postRequestForResponse
import com.safehill.kclient.network.remote.S3Proxy.upload
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AssetUploader(
    baseApi: BaseApi,
    private val safehillLogger: SafehillLogger
) : BaseApi by baseApi {

    suspend fun upload(
        assets: List<EncryptedAsset>,
        groupId: GroupId
    ) {
        if (assets.size > 1) {
            throw NotImplementedError("Current API only supports creating one asset per request")
        }
        val asset = assets.first()
        val assetCreatedAt = asset.creationDate ?: run { Instant.MIN }
        val dateTime = OffsetDateTime.ofInstant(assetCreatedAt, ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

        val requestBody = AssetInputDTO(
            globalIdentifier = asset.globalIdentifier,
            localIdentifier = asset.localIdentifier,
            creationDate = dateTime.format(formatter),
            groupId = groupId,
            versions = asset.encryptedVersions.map {
                AssetVersionInputDTO(
                    versionName = it.key.versionName,
                    senderEncryptedSecret = it.value.encryptedSecret.base64EncodedString(),
                    ephemeralPublicKey = it.value.publicKeyData.base64EncodedString(),
                    publicSignature = it.value.publicSignatureData.base64EncodedString(),
                )
            },
            force = true,
            perceptualHash = asset.fingerPrint?.assetHash,
            embeddings = asset.fingerPrint?.embeddings?.toServerRepresentation()
        )
        val shOutput: AssetOutputDTO = postRequestForResponse(
            endPoint = "/assets/create",
            request = requestBody
        )
        uploadEncryptedAssetToS3Bucket(serverAsset = shOutput, asset = asset)
    }

    private fun Embeddings.toServerRepresentation(): String {
        val byteBuffer = ByteBuffer.allocate(this.size * 4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN) // Match platform endianness
        this.forEach {
            byteBuffer.putFloat(it)
        }
        return byteBuffer.array().base64EncodedString()
    }


    private suspend fun uploadEncryptedAssetToS3Bucket(
        serverAsset: AssetOutputDTO,
        asset: EncryptedAsset,
    ) {
        try {
            val encryptedVersionWithPresignedURL =
                serverAsset.versions.associateWithNotNull { assetVersion ->
                    val quality = AssetQuality.fromVersionName(assetVersion.versionName)
                    asset.encryptedVersions[quality]
                }
            uploadEachVersions(
                serverAsset = serverAsset,
                encryptedVersionByPresignedURL = encryptedVersionWithPresignedURL
            )

        } catch (exception: Exception) {
            safehillLogger.error(
                "Error while uploading asset ${asset.globalIdentifier}. Exception = $exception"
            )
            throw exception
        }
    }

    private suspend fun uploadEachVersions(
        serverAsset: AssetOutputDTO,
        encryptedVersionByPresignedURL: Map<AssetVersionOutputDTO, EncryptedAssetVersion>
    ) {
        coroutineScope {
            encryptedVersionByPresignedURL.forEach { (assetVersionOutputDto, encryptedVersion) ->
                launch {
                    try {
                        upload(
                            data = encryptedVersion.encryptedData,
                            url = assetVersionOutputDto.presignedURL
                        )
                        markAssetAsUploaded(
                            serverAsset.globalIdentifier,
                            encryptedVersion.quality,
                        )
                    } catch (exception: Exception) {
                        safehillLogger.error("Error while uploading asset version ${encryptedVersion.quality}, Exception = $exception")
                        throw exception
                    }
                }
            }
        }
    }

    private suspend fun markAssetAsUploaded(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
    ) {
        postRequest(
            endPoint = "assets/$assetGlobalIdentifier/versions/${quality.versionName}/uploaded",
            request = null
        )
    }

    private fun <T, R> Collection<T>.associateWithNotNull(valueSelector: (T) -> R?): Map<T, R> =
        this.mapNotNull { element ->
            val value = valueSelector(element)
            if (value != null) element to value else null
        }.toMap()

}