package com.safehill.kclient.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.safehill.kclient.api.dtos.*
import com.safehill.kclient.api.serde.toIso8601String
import com.safehill.kclient.models.*
import com.safehill.kcrypto.SHCypher
import com.safehill.kcrypto.models.SHRemoteCryptoUser
import com.safehill.kcrypto.models.SHShareablePayload
import java.security.MessageDigest
import java.util.*


enum class ServerEnvironment {
    Production, Development
}

enum class SHHTTPStatusCode(val statusCode: Int) {
    UNAUTHORIZED(401),
    PAYMENT_REQUIRED(402),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409);

    companion object {
        fun fromInt(value: Int): SHHTTPStatusCode? {
            return entries.firstOrNull() { it.statusCode == value }
        }
    }
}

data class SHHTTPException(
    val statusCode: SHHTTPStatusCode?,
    override val message: String,
) : Exception(message) {
    constructor(statusCode: Int, message: String)
            : this(SHHTTPStatusCode.fromInt(statusCode), "$statusCode: $message")
}


// For Fuel howto see https://www.baeldung.com/kotlin/fuel

class SHHTTPAPI(
    override var requestor: SHLocalUser,
    private val environment: ServerEnvironment = ServerEnvironment.Development,
    hostname: String = "localhost"
) : SHSafehillAPI {

    init {
        FuelManager.instance.basePath = when (this.environment) {
            ServerEnvironment.Development -> "http://${hostname}:8080"
            ServerEnvironment.Production -> "https://app.safehill.io:433"
        }
        FuelManager.instance.baseHeaders = mapOf("Content-type" to "application/json")
        FuelManager.instance.timeoutInMillisecond = 10000
        FuelManager.instance.timeoutReadInMillisecond = 30000
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
            .handleResponse()
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
            .handleResponse()

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
        val (request, response, result) = "/users/update".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(requestBody))
            .responseObject(SHRemoteUser.Deserializer())

        println(
            "[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                    "response.status=${response.statusCode}"
        )

        when (result) {
            is Result.Success -> return result.component1()!!
            is Result.Failure -> {
                throw SHHTTPException(response.statusCode, response.responseMessage)
            }
        }
    }

    @Throws
    override suspend fun deleteAccount(name: String, password: String) {
        TODO("Not yet implemented")
    }

    @Throws
    override suspend fun deleteAccount() {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val (request, response, result) = "/users/safe_delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseString()

        println("[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                "response.status=${response.statusCode}")

        when (result) {
            is Result.Success -> return
            is Result.Failure ->
                throw SHHTTPException(response.statusCode, response.responseMessage)
        }
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
            identifier=requestor.identifier,
        )
        val (startRequest, startResponse, startResult) = "/signin/challenge/start".httpPost()
            .body(Gson().toJson(authRequestBody))
            .responseObject(SHAuthChallengeResponseDTO.Deserializer())

        println("[api] POST url=${startRequest.url} with headers=${startRequest.header()} body=${startRequest.body} " +
                "response.status=${startResponse.statusCode}")

        val authChallenge = when (startResult) {
            is Result.Success -> startResult.component1()!!
            is Result.Failure -> {
                throw SHHTTPException(startResponse.statusCode, startResponse.responseMessage)
            }
        }

        val solvedChallenge = this.solveChallenge(authChallenge)

        val (verifyRequest, verifyResponse, verifyResult) = "/signin/challenge/verify".httpPost()
            .body(Gson().toJson(solvedChallenge))
            .responseObject(SHAuthResponseDTO.Deserializer())

        println("[api] POST url=${verifyRequest.url} with headers=${verifyRequest.header()} body=${verifyRequest.body} " +
                "response.status=${verifyResponse.statusCode}")

        when (verifyResult) {
            is Result.Success -> return verifyResult.component1()!!
            is Result.Failure -> {
                throw SHHTTPException(verifyResponse.statusCode, verifyResponse.responseMessage)
            }
        }
    }

    @Throws
    override suspend fun getUsers(withIdentifiers: List<String>): List<SHRemoteUser> {
        val bearerToken = this.requestor.authToken ?: throw SHHTTPException(401, "unauthorized")

        if (withIdentifiers.isEmpty()) { return listOf() }

        val getUsersRequestBody = SHUserIdentifiersDTO(userIdentifiers = withIdentifiers)
        val (getRequest, getResponse, getResult) = "/users/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(getUsersRequestBody))
            .responseObject(SHRemoteUser.ListDeserializer())

        println("[api] POST url=${getRequest.url} with headers=${getRequest.header()} body=${getRequest.body} " +
                "response.status=${getResponse.statusCode}")

        when (getResult) {
            is Result.Success -> return getResult.component1()!!
            is Result.Failure ->
                throw SHHTTPException(getResponse.statusCode, getResponse.responseMessage)
        }
    }

    @Throws
    override suspend fun searchUsers(query: String): List<SHRemoteUser> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val getUsersRequestBody = SHUserSearchDTO(query, per = 5, page = 1)
        val (searchRequest, searchResponse, searchResult) = "/users/search".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(getUsersRequestBody))
            .responseObject(SHRemoteUser.ListDeserializer())

        println("[api] POST url=${searchRequest.url} with headers=${searchRequest.header()} body=${searchRequest.body} " +
                "response.status=${searchResponse.statusCode}")

        when (searchResult) {
            is Result.Success -> return searchResult.component1()!!
            is Result.Failure ->
                throw SHHTTPException(searchResponse.statusCode, searchResponse.responseMessage)
        }
    }

    @Throws
    override suspend fun getAssetDescriptors(): List<SHAssetDescriptor> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val (request, response, result) = "/assets/descriptors/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseObject(SHAssetDescriptor.ListDeserializer())

        println("[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                "response.status=${response.statusCode}")

        when (result) {
            is Result.Success -> return result.component1()!!
            is Result.Failure -> {
                throw SHHTTPException(response.statusCode, response.responseMessage)
            }
        }
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
        val (request, response, result) = "/assets/create".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(requestBody))
            .responseObject(SHAssetOutputDTO.Deserializer())

        println("[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                "response.status=${response.statusCode}")

        when (result) {
            is Result.Success -> return listOf(result.component1()!!)
            is Result.Failure -> {
                throw SHHTTPException(response.statusCode, response.responseMessage)
            }
        }
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

        val (request, response, _) = "/assets/delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(SHAssetDeleteCriteriaDTO(globalIdentifiers)))
            .response()

        println("[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                "response.status=${response.statusCode}")

        when (response.statusCode) {
            200 -> return globalIdentifiers
            else -> throw SHHTTPException(response.statusCode, response.responseMessage)
        }
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

    override suspend fun addReactions(reactions: List<SHUserReaction>, toGroupId: String): List<SHReactionOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun removeReaction(reaction: SHUserReaction, fromGroupId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveInteractions(inGroupId: String, per: Int, page: Int): List<SHInteractionsGroupDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun addMessages(messages: List<SHMessageInputDTO>, toGroupId: String): List<SHMessageOutputDTO> {
        TODO("Not yet implemented")
    }

    private fun <T> ResponseResultOf<T>.handleResponse(): T {
        log()
        return getOrThrow()
    }

    private fun <T> ResponseResultOf<T>.getOrThrow(): T {
        return when (val result = this.third) {
            is Result.Success -> result.value
            is Result.Failure -> {
                val fuelError = result.error
                throw if (fuelError.exception is HttpException) {
                    SHHTTPException(
                        fuelError.response.statusCode,
                        fuelError.response.responseMessage
                    )
                } else {
                    result.error.exception
                }
            }
        }
    }

    private fun <T> ResponseResultOf<T>.log() {
        val (request, response, result) = this
        println(
            "[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                    "response.status=${response.statusCode}"
        )

    }


}

