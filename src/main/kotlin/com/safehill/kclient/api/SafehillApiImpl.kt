package com.safehill.kclient.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.safehill.kclient.api.dtos.HashedPhoneNumber
import com.safehill.kclient.api.dtos.SHAssetDeleteCriteriaDTO
import com.safehill.kclient.api.dtos.SHAssetInputDTO
import com.safehill.kclient.api.dtos.SHAssetOutputDTO
import com.safehill.kclient.api.dtos.SHAssetVersionInputDTO
import com.safehill.kclient.api.dtos.SHAuthChallengeRequestDTO
import com.safehill.kclient.api.dtos.SHAuthChallengeResponseDTO
import com.safehill.kclient.api.dtos.SHAuthResolvedChallengeDTO
import com.safehill.kclient.api.dtos.SHAuthResponseDTO
import com.safehill.kclient.api.dtos.SHInteractionsGroupDTO
import com.safehill.kclient.api.dtos.SHMessageInputDTO
import com.safehill.kclient.api.dtos.SHMessageOutputDTO
import com.safehill.kclient.api.dtos.SHReactionOutputDTO
import com.safehill.kclient.api.dtos.SHRecipientEncryptionDetailsDTO
import com.safehill.kclient.api.dtos.SHSendCodeToUserRequestDTO
import com.safehill.kclient.api.dtos.SHUserIdentifiersDTO
import com.safehill.kclient.api.dtos.SHUserInputDTO
import com.safehill.kclient.api.dtos.SHUserSearchDTO
import com.safehill.kclient.api.dtos.SHUserUpdateDTO
import com.safehill.kclient.api.dtos.UserPhoneNumbersDTO
import com.safehill.kclient.api.serde.SHRemoteUserMapDeserializer
import com.safehill.kclient.api.serde.toIso8601String
import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHAssetDescriptorUploadState
import com.safehill.kclient.models.SHAssetQuality
import com.safehill.kclient.models.SHEncryptedAsset
import com.safehill.kclient.models.SHLocalUser
import com.safehill.kclient.models.SHRemoteUser
import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.SHShareableEncryptedAsset
import com.safehill.kclient.models.SHUserReaction
import com.safehill.kcrypto.SHCypher
import com.safehill.kcrypto.models.SHRemoteCryptoUser
import com.safehill.kcrypto.models.SHShareablePayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Base64
import java.util.Date


enum class ServerEnvironment {
    Production, Development
}

enum class SafehillHttpStatusCode(val statusCode: Int) {
    UnAuthorized(401),
    PaymentRequired(402),
    NotFound(404),
    MethodNotAllowed(405),
    Conflict(409);

    companion object {
        fun fromInt(value: Int): SafehillHttpStatusCode? {
            return entries.firstOrNull() { it.statusCode == value }
        }
    }
}

data class SafehillHttpException(
    val statusCode: SafehillHttpStatusCode?,
    override val message: String,
    val httpException: HttpException
) : Exception(message, httpException) {
    constructor(
        statusCode: Int,
        message: String,
        httpException: HttpException
    ) : this(
        SafehillHttpStatusCode.fromInt(statusCode),
        "$statusCode: $message",
        httpException
    )
}


// For Fuel howto see https://www.baeldung.com/kotlin/fuel

class SafehillApiImpl(
    override var requestor: SHLocalUser,
    private val environment: ServerEnvironment = ServerEnvironment.Development,
    hostname: String = "localhost"
) : SafehillApi {

    init {
        FuelManager.instance.basePath = when (this.environment) {
            ServerEnvironment.Development -> "http://${hostname}:8080"
            ServerEnvironment.Production -> "https://app.safehill.io:433"
        }
        FuelManager.instance.baseHeaders = mapOf("Content-type" to "application/json")
        FuelManager.instance.timeoutInMillisecond = 10000
        FuelManager.instance.timeoutReadInMillisecond = 30000

        // The client should control whether they want logging or not
        // Printing for now
        FuelManager.instance.addRequestInterceptor(LogRequestInterceptor)
        FuelManager.instance.addResponseInterceptor(LogResponseInterceptor)
    }

    @Throws
    override suspend fun createUser(name: String): SHServerUser {
        val requestBody = SHUserInputDTO(
            identifier = requestor.identifier,
            publicKey = Base64.getEncoder().encodeToString(requestor.publicKeyData),
            publicSignature = Base64.getEncoder().encodeToString(requestor.publicSignatureData),
            name = name
        )
        return "/users/create".httpPost()
            .body(Gson().toJson(requestBody))
            .responseObject(SHRemoteUser.Deserializer())
            .getOrThrow()
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SHSendCodeToUserRequestDTO.Medium,
    ) {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val requestBody = SHSendCodeToUserRequestDTO(
            countryCode = countryCode,
            phoneNumber = phoneNumber,
            code = code,
            medium = medium
        )

        "/users/code/send".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(requestBody))
            .response()
            .getOrThrow()

    }

    @Throws
    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?,
    ): SHServerUser {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val requestBody = SHUserUpdateDTO(
            identifier = null,
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            publicKey = null,
            publicSignature = null
        )
        return "/users/update".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(requestBody))
            .responseObject(SHRemoteUser.Deserializer())
            .getOrThrow()
    }

    @Throws
    override suspend fun deleteAccount(name: String, password: String) {
        TODO("Not yet implemented")
    }

    @Throws
    override suspend fun deleteAccount() {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        "/users/safe_delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseString()
            .getOrThrow()
    }

    @Throws
    fun solveChallenge(authChallenge: SHAuthChallengeResponseDTO): SHAuthResolvedChallengeDTO {
        val serverCrypto = SHRemoteCryptoUser(
            Base64.getDecoder().decode(authChallenge.publicKey),
            Base64.getDecoder().decode(authChallenge.publicSignature)
        )
        val encryptedChallenge = SHShareablePayload(
            ephemeralPublicKeyData = Base64.getDecoder().decode(authChallenge.ephemeralPublicKey),
            ciphertext = Base64.getDecoder().decode(authChallenge.challenge),
            signature = Base64.getDecoder().decode(authChallenge.ephemeralPublicSignature),
            null
        )

        val decryptedChallenge = SHCypher.decrypt(
            sealedMessage = encryptedChallenge,
            encryptionKey = this.requestor.shUser.key,
            signedBy = serverCrypto.publicSignature,
            iv = authChallenge.iv?.let { Base64.getDecoder().decode(it) },
            protocolSalt = Base64.getDecoder().decode(authChallenge.protocolSalt)
        )
        val signatureForChallenge = this.requestor.shUser.sign(decryptedChallenge)
        val md = MessageDigest.getInstance("SHA-512")
        val digest512 = md.digest(decryptedChallenge)
        val signatureForDigest = this.requestor.shUser.sign(digest512)

        return SHAuthResolvedChallengeDTO(
            userIdentifier = requestor.identifier,
            signedChallenge = Base64.getEncoder().encodeToString(signatureForChallenge),
            digest = Base64.getEncoder().encodeToString(digest512),
            signedDigest = Base64.getEncoder().encodeToString(signatureForDigest)
        )
    }

    @Throws
    override suspend fun signIn(): SHAuthResponseDTO {
        val authRequestBody = SHAuthChallengeRequestDTO(
            identifier = requestor.identifier,
        )
        val authChallenge = "/signin/challenge/start".httpPost()
            .body(Gson().toJson(authRequestBody))
            .responseObject(SHAuthChallengeResponseDTO.Deserializer())
            .getOrThrow()


        val solvedChallenge = this.solveChallenge(authChallenge)

        return "/signin/challenge/verify".httpPost()
            .body(Gson().toJson(solvedChallenge))
            .responseObject(SHAuthResponseDTO.Deserializer())
            .getOrThrow()

    }

    @Throws
    override suspend fun getUsers(withIdentifiers: List<String>): List<SHRemoteUser> {
        val bearerToken =
            this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        if (withIdentifiers.isEmpty()) {
            return listOf()
        }

        val getUsersRequestBody = SHUserIdentifiersDTO(userIdentifiers = withIdentifiers)
        return "/users/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(getUsersRequestBody))
            .responseObject(SHRemoteUser.ListDeserializer())
            .getOrThrow()
    }

    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<String>): Map<HashedPhoneNumber, SHRemoteUser> {
        val bearerToken =
            this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        if (hashedPhoneNumbers.isEmpty()) {
            return mapOf()
        }

        val getUsersRequestBody = UserPhoneNumbersDTO(phoneNumbers = hashedPhoneNumbers)
        return "/users/retrieve/phone-number".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(getUsersRequestBody))
            .responseObject(SHRemoteUserMapDeserializer())
            .getOrThrow()
    }

    @Throws
    override suspend fun searchUsers(query: String): List<SHRemoteUser> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val getUsersRequestBody = SHUserSearchDTO(query, per = 5, page = 1)
        return "/users/search".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(getUsersRequestBody))
            .responseObject(SHRemoteUser.ListDeserializer())
            .getOrThrow()

    }

    @Throws
    override suspend fun getAssetDescriptors(): List<SHAssetDescriptor> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        return "/assets/descriptors/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseObject(SHAssetDescriptor.ListDeserializer())
            .getOrThrow()
    }

    @Throws
    override suspend fun getAssetDescriptors(assetGlobalIdentifiers: List<AssetGlobalIdentifier>): List<SHAssetDescriptor> {
        TODO("Not yet implemented")
    }

    @Throws
    override suspend fun getAssets(
        globalIdentifiers: List<String>,
        versions: List<SHAssetQuality>?,
    ): Map<String, SHEncryptedAsset> {
        TODO("Not yet implemented")
    }

    @Throws
    override suspend fun create(
        assets: List<SHEncryptedAsset>,
        groupId: String,
        filterVersions: List<SHAssetQuality>?,
    ): List<SHAssetOutputDTO> {
        if (assets.size > 1) {
            throw NotImplementedError("Current API only supports creating one asset per request")
        }
        val asset = assets.first()

        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val assetCreatedAt = asset.creationDate ?: run { Date(0) }
        val requestBody = SHAssetInputDTO(
            asset.globalIdentifier,
            asset.localIdentifier,
            assetCreatedAt.toIso8601String(),
            groupId,
            asset.encryptedVersions.map {
                SHAssetVersionInputDTO(
                    it.key.toString(),
                    Base64.getEncoder().encodeToString(it.value.publicKeyData),
                    Base64.getEncoder().encodeToString(it.value.publicSignatureData),
                    Base64.getEncoder().encodeToString(it.value.encryptedSecret),
                )
            },
            forceUpdateVersions = true
        )
        val shOutput = "/assets/create".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(requestBody))
            .responseObject(SHAssetOutputDTO.Deserializer())
            .getOrThrow()
        return listOf(shOutput)
    }

    override suspend fun share(asset: SHShareableEncryptedAsset) {
        TODO("Not yet implemented")
    }

    override suspend fun unshare(assetId: AssetGlobalIdentifier, userPublicIdentifier: String) {
        TODO("Not yet implemented")
    }

    override suspend fun upload(
        serverAsset: SHAssetOutputDTO,
        asset: SHEncryptedAsset,
        filterVersions: List<SHAssetQuality>,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: SHAssetQuality,
        asState: SHAssetDescriptorUploadState,
    ) {
        TODO("Not yet implemented")
    }

    @Throws
    override suspend fun deleteAssets(globalIdentifiers: List<String>): List<String> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val responseResult = "/assets/delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(SHAssetDeleteCriteriaDTO(globalIdentifiers)))
            .response()
        return responseResult.getMappingOrThrow { globalIdentifiers }
    }

    override suspend fun setGroupEncryptionDetails(
        groupId: String,
        recipientsEncryptionDetails: List<SHRecipientEncryptionDetailsDTO>,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteGroup(groupId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: String): List<SHRecipientEncryptionDetailsDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun addReactions(
        reactions: List<SHUserReaction>,
        toGroupId: String
    ): List<SHReactionOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun removeReaction(reaction: SHUserReaction, fromGroupId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveInteractions(
        inGroupId: String,
        per: Int,
        page: Int
    ): List<SHInteractionsGroupDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun addMessages(
        messages: List<SHMessageInputDTO>,
        toGroupId: String
    ): List<SHMessageOutputDTO> {
        TODO("Not yet implemented")
    }

    private fun <T, R> ResponseResultOf<T>.getMappingOrThrow(transform: (T) -> R): R {
        val value = getOrThrow()
        return transform(value)
    }

    private fun <T> ResponseResultOf<T>.getOrThrow(): T {
        return when (val result = this.third) {
            is Result.Success -> result.value
            is Result.Failure -> {
                val fuelError = result.error
                val exception = fuelError.exception
                throw if (exception is HttpException) {
                    SafehillHttpException(
                        fuelError.response.statusCode,
                        fuelError.response.responseMessage,
                        exception
                    )
                } else {
                    exception
                }
            }
        }
    }


}

