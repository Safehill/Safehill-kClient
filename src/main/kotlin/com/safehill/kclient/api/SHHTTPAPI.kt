package com.safehill.kclient.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
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


// For Fuel howto see https://www.baeldung.com/kotlin/fuel

class SHHTTPAPI(
    override var requestor: SHLocalUser,
    private val environment: ServerEnvironment = ServerEnvironment.Development
) : SHSafehillAPI {

    init {
        FuelManager.instance.basePath = when(this.environment) {
            ServerEnvironment.Development -> "http://localhost:8080"
            ServerEnvironment.Production -> "https://app.safehill.io:433"
        }
        FuelManager.instance.baseHeaders = mapOf("Content-type" to "application/json")
        FuelManager.instance.timeoutInMillisecond = 10000
        FuelManager.instance.timeoutReadInMillisecond = 30000
    }

    @Throws
    override suspend fun createUser(name: String): SHServerUser {
        val requestBody = SHCreateUserRequest(
            identifier=requestor.identifier,
            publicKey=Base64.getEncoder().encodeToString(requestor.publicKeyData),
            publicSignature=Base64.getEncoder().encodeToString(requestor.publicSignatureData),
            name=name
        )
        val (request, response, result) = "/users/create".httpPost()
            .body(Gson().toJson(requestBody))
            .responseObject(SHRemoteUser.Deserializer())

        println("[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                "response.status=${response.statusCode}")

        when (result) {
            is Result.Success -> return result.component1()!!
            is Result.Failure -> {
                throw HttpException(response.statusCode, response.responseMessage)
            }
        }
    }

    override suspend fun updateUser(name: String?): SHServerUser {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(name: String, password: String) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount() {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val (request, response, result) = "/users/safe_delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseString()

        println("[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                "response.status=${response.statusCode}")

        when (result) {
            is Result.Success -> return
            is Result.Failure -> throw result.getException()
        }
    }

    fun solveChallenge(authChallenge: SHAuthChallenge): SHAuthSolvedChallenge {
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

        return SHAuthSolvedChallenge(
            userIdentifier = requestor.identifier,
            signedChallenge = Base64.getEncoder().encodeToString(signatureForChallenge),
            digest = Base64.getEncoder().encodeToString(digest512),
            signedDigest = Base64.getEncoder().encodeToString(signatureForDigest)
        )
    }

    override suspend fun signIn(name: String): SHAuthResponse {
        val authRequestBody = SHAuthStartChallenge(
            identifier=requestor.identifier,
            name=name
        )
        val (startRequest, startResponse, startResult) = "/signin/challenge/start".httpPost()
            .body(Gson().toJson(authRequestBody))
            .responseObject(SHAuthChallenge.Deserializer())

        println("[api] POST url=${startRequest.url} with headers=${startRequest.header()} body=${startRequest.body} " +
                "response.status=${startResponse.statusCode}")

        val authChallenge = when (startResult) {
            is Result.Success -> startResult.component1()!!
            is Result.Failure -> {
//                println("Error: ${result.error}")
                throw HttpException(startResponse.statusCode, startResponse.responseMessage)
            }
        }

        val solvedChallenge = this.solveChallenge(authChallenge)

        val (verifyRequest, verifyResponse, verifyResult) = "/signin/challenge/verify".httpPost()
            .body(Gson().toJson(solvedChallenge))
            .responseObject(SHAuthResponse.Deserializer())

        println("[api] POST url=${verifyRequest.url} with headers=${verifyRequest.header()} body=${verifyRequest.body} " +
                "response.status=${verifyResponse.statusCode}")

        when (verifyResult) {
            is Result.Success -> return verifyResult.component1()!!
            is Result.Failure -> {
//                println("Error: ${result.error}")
                throw HttpException(verifyResponse.statusCode, verifyResponse.responseMessage)
            }
        }
    }

    override suspend fun getUsers(withIdentifiers: List<String>): List<SHRemoteUser> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        if (withIdentifiers.isEmpty()) { return listOf() }

        val getUsersRequestBody = SHGetUsersRequest(userIdentifiers = withIdentifiers)
        val (getRequest, getResponse, getResult) = "/users/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(getUsersRequestBody))
            .responseObject(SHRemoteUser.ListDeserializer())

        println("[api] POST url=${getRequest.url} with headers=${getRequest.header()} body=${getRequest.body} " +
                "response.status=${getResponse.statusCode}")

        when (getResult) {
            is Result.Success -> return getResult.component1()!!
            is Result.Failure ->
                throw HttpException(getResponse.statusCode, getResponse.responseMessage)
        }
    }

    override suspend fun searchUsers(query: String): List<SHRemoteUser> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val getUsersRequestBody = SHSearchUsersRequest(query, per = 5, page = 1)
        val (searchRequest, searchResponse, searchResult) = "/users/search".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(getUsersRequestBody))
            .responseObject(SHRemoteUser.ListDeserializer())

        println("[api] POST url=${searchRequest.url} with headers=${searchRequest.header()} body=${searchRequest.body} " +
                "response.status=${searchResponse.statusCode}")

        when (searchResult) {
            is Result.Success -> return searchResult.component1()!!
            is Result.Failure ->
                throw HttpException(searchResponse.statusCode, searchResponse.responseMessage)
        }
    }

    override suspend fun getAssetDescriptors(): List<SHAssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(assetGlobalIdentifiers: List<AssetGlobalIdentifier>): List<SHAssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssets(
        withGlobalIdentifiers: List<String>,
        versions: List<SHAssetQuality>?,
    ): Map<String, SHEncryptedAsset> {
        TODO("Not yet implemented")
    }

    override suspend fun create(
        assets: List<SHEncryptedAsset>,
        groupId: String,
        filterVersions: List<SHAssetQuality>?,
    ): List<SHServerAsset> {
        if (assets.size > 1) {
            throw NotImplementedError("Current API only supports creating one asset per request")
        }
        val asset = assets.first()

        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val assetCreatedAt = asset.creationDate ?: run { Date(0) }
        val requestBody = SHCreateAssetRequest(
            asset.globalIdentifier,
            asset.localIdentifier,
            assetCreatedAt.toIso8601String(),
            groupId,
            asset.encryptedVersions.map {
                SHCreateServerAssetVersion(
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
            .responseObject(SHServerAsset.Deserializer())

        println("[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                "response.status=${response.statusCode}")

        when (result) {
            is Result.Success -> return listOf(result.component1()!!)
            is Result.Failure -> {
                throw HttpException(response.statusCode, response.responseMessage)
            }
        }
    }

    override suspend fun deleteAssets(withGlobalIdentifiers: List<String>): List<String> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val (request, response, _) = "/assets/delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(SHDeleteAssetRequest(withGlobalIdentifiers)))
            .response()

        println("[api] POST url=${request.url} with headers=${request.header()} body=${request.body} " +
                "response.status=${response.statusCode}")

        when (response.statusCode) {
            200 -> return withGlobalIdentifiers
            else -> throw HttpException(response.statusCode, response.responseMessage)
        }
    }

}
