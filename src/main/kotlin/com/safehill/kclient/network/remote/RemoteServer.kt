package com.safehill.kclient.network.remote

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.serialization.responseObject
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.safehill.kclient.models.assets.AssetDescriptorUploadState
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.dtos.*
import com.safehill.kclient.models.serde.toIso8601String
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.SafehillApi
import com.safehill.kclient.network.exceptions.SafehillHttpException
import com.safehill.kclient.network.exceptions.UnauthorizedSafehillHttpException
import com.safehill.kcrypto.SafehillCypher
import com.safehill.kcrypto.models.RemoteCryptoUser
import com.safehill.kcrypto.models.ShareablePayload
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Base64
import java.util.Date


// For Fuel how to see https://www.baeldung.com/kotlin/fuel

class RemoteServer(
    override var requestor: LocalUser,
    private val environment: RemoteServerEnvironment = RemoteServerEnvironment.Development,
    hostname: String = "localhost"
) : SafehillApi {

    // todo properly setup fuel configurations only once
    companion object {
        var alreadyInstantiated = false
    }

    init {
        if (!alreadyInstantiated) {
            FuelManager.instance.basePath = when (this.environment) {
                RemoteServerEnvironment.Development -> "http://${hostname}:8080"
                RemoteServerEnvironment.Production -> "https://app.safehill.io:443"
            }
            FuelManager.instance.baseHeaders = mapOf("Content-type" to "application/json")
            FuelManager.instance.timeoutInMillisecond = 10000
            FuelManager.instance.timeoutReadInMillisecond = 30000

            // The client should control whether they want logging or not
            // Printing for now
            FuelManager.instance.addRequestInterceptor(LogRequestInterceptor)
            FuelManager.instance.addResponseInterceptor(LogResponseInterceptor)
            alreadyInstantiated = true
        }
    }

    @Throws
    override suspend fun createUser(name: String): ServerUser {
        val requestBody = UserInputDTO(
            identifier = requestor.identifier,
            publicKey = Base64.getEncoder().encodeToString(requestor.publicKeyData),
            publicSignature = Base64.getEncoder().encodeToString(requestor.publicSignatureData),
            name = name
        )
        return "/users/create".httpPost()
            .body(Gson().toJson(requestBody))
            .responseObject(RemoteUser.Deserializer())
            .getOrThrow()
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SendCodeToUserRequestDTO.Medium,
    ) {
        val bearerToken = this.requestor.authToken ?: throw UnauthorizedSafehillHttpException

        val requestBody = SendCodeToUserRequestDTO(
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
    ): ServerUser {
        val bearerToken = this.requestor.authToken ?: throw UnauthorizedSafehillHttpException

        val requestBody = UserUpdateDTO(
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
            .responseObject(RemoteUser.Deserializer())
            .getOrThrow()
    }

    @Throws
    override suspend fun deleteAccount() {
        val bearerToken = this.requestor.authToken ?: throw UnauthorizedSafehillHttpException

        "/users/safe_delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseString()
            .getOrThrow()
    }

    @Throws
    fun solveChallenge(authChallenge: AuthChallengeResponseDTO): AuthResolvedChallengeDTO {
        val serverCrypto = RemoteCryptoUser(
            Base64.getDecoder().decode(authChallenge.publicKey),
            Base64.getDecoder().decode(authChallenge.publicSignature)
        )
        val encryptedChallenge = ShareablePayload(
            ephemeralPublicKeyData = Base64.getDecoder()
                .decode(authChallenge.ephemeralPublicKey),
            ciphertext = Base64.getDecoder().decode(authChallenge.challenge),
            signature = Base64.getDecoder().decode(authChallenge.ephemeralPublicSignature),
            null
        )

        val decryptedChallenge = SafehillCypher.decrypt(
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

        return AuthResolvedChallengeDTO(
            userIdentifier = requestor.identifier,
            signedChallenge = Base64.getEncoder().encodeToString(signatureForChallenge),
            digest = Base64.getEncoder().encodeToString(digest512),
            signedDigest = Base64.getEncoder().encodeToString(signatureForDigest)
        )
    }

    @Throws
    override suspend fun signIn(): AuthResponseDTO {
        val authRequestBody = AuthChallengeRequestDTO(
            identifier = requestor.identifier,
        )
        val authChallenge = "/signin/challenge/start".httpPost()
            .body(Gson().toJson(authRequestBody))
            .responseObject(AuthChallengeResponseDTO.Deserializer())
            .getOrThrow()


        val solvedChallenge = this.solveChallenge(authChallenge)

        return "/signin/challenge/verify".httpPost()
            .body(Gson().toJson(solvedChallenge))
            .responseObject(AuthResponseDTO.Deserializer())
            .getOrThrow()

    }

    @Throws
    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, RemoteUser> {
        val bearerToken =
            this.requestor.authToken ?: throw UnauthorizedSafehillHttpException

        if (withIdentifiers.isEmpty()) {
            return emptyMap()
        }

        val getUsersRequestBody = UserIdentifiersDTO(userIdentifiers = withIdentifiers)
        return "/users/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(getUsersRequestBody))
            .responseObject(
                ListSerializer(RemoteUser.serializer()),
                Json {
                    this.ignoreUnknownKeys = true
                }
            )
            .getOrThrow()
            .associateBy { it.identifier }
    }

    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, RemoteUser> {
        val bearerToken =
            this.requestor.authToken ?: throw UnauthorizedSafehillHttpException

        if (hashedPhoneNumbers.isEmpty()) {
            return mapOf()
        }

        val getUsersRequestBody = UserPhoneNumbersDTO(phoneNumbers = hashedPhoneNumbers)
        return "/users/retrieve/phone-number".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(getUsersRequestBody))
            .responseObject<RemoteUserPhoneNumberMatchDto>()
            .getOrThrow()
            .result
    }

    @Throws
    override suspend fun searchUsers(query: String, per: Int, page: Int): List<RemoteUser> {
        val bearerToken = this.requestor.authToken ?: throw UnauthorizedSafehillHttpException

        return "/users/search".httpGet(
            listOf(
                "query" to query,
                "per" to per,
                "page" to page
            )
        )
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseObject<RemoteUserSearchDTO>()
            .getOrThrow()
            .items
    }

    @Throws
    override suspend fun getAssetDescriptors(after: Date?): List<AssetDescriptor> {
        val bearerToken = this.requestor.authToken ?: throw UnauthorizedSafehillHttpException
        val descriptorFilterCriteriaDTO = AssetDescriptorFilterCriteriaDTO(
            after = after?.toIso8601String(),
            globalIdentifiers = null,
            groupIds = null
        )

        return "/assets/descriptors/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(descriptorFilterCriteriaDTO))
            .responseObject(AssetDescriptor.ListDeserializer())
            .getOrThrow()
    }

    @Throws
    override suspend fun getAssetDescriptors(
        assetGlobalIdentifiers: List<AssetGlobalIdentifier>?,
        groupIds: List<GroupId>?,
        after: Date?
    ): List<AssetDescriptor> {
        val bearerToken = this.requestor.authToken ?: throw UnauthorizedSafehillHttpException
        val descriptorFilterCriteriaDTO = AssetDescriptorFilterCriteriaDTO(
            after = after?.toIso8601String(),
            globalIdentifiers = assetGlobalIdentifiers,
            groupIds = groupIds
        )

        return "/assets/descriptors/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(descriptorFilterCriteriaDTO))
            .responseObject(AssetDescriptor.ListDeserializer())
            .getOrThrow()
    }

    @Throws
    override suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>?,
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        TODO("Not yet implemented")
    }

    @Throws
    override suspend fun create(
        assets: List<EncryptedAsset>,
        groupId: GroupId,
        filterVersions: List<AssetQuality>?,
    ): List<com.safehill.kclient.models.dtos.AssetOutputDTO> {
        if (assets.size > 1) {
            throw NotImplementedError("Current API only supports creating one asset per request")
        }
        val asset = assets.first()

        val bearerToken = this.requestor.authToken ?: throw UnauthorizedSafehillHttpException

        val assetCreatedAt = asset.creationDate ?: run { Date(0) }
        val requestBody = com.safehill.kclient.models.dtos.AssetInputDTO(
            asset.globalIdentifier,
            asset.localIdentifier,
            assetCreatedAt.toIso8601String(),
            groupId,
            asset.encryptedVersions.map {
                com.safehill.kclient.models.dtos.AssetVersionInputDTO(
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
            .responseObject(com.safehill.kclient.models.dtos.AssetOutputDTO.Deserializer())
            .getOrThrow()
        return listOf(shOutput)
    }

    override suspend fun share(asset: ShareableEncryptedAsset) {
        TODO("Not yet implemented")
    }

    override suspend fun unshare(assetId: AssetGlobalIdentifier, userPublicIdentifier: UserIdentifier) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveThread(usersIdentifiers: List<UserIdentifier>): ConversationThreadOutputDTO? {
        return listThreads(usersIdentifiers).firstOrNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        return "/threads/retrieve/".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseObject(
                ConversationThreadOutputDTO.serializer(),
                Json {
                    explicitNulls = false
                }
            )
            .getOrElseOnSafehillException {
                if (it.statusCode == SafehillHttpStatusCode.NotFound) {
                    null
                } else {
                    throw it
                }
            }
    }


    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return listThreads(null)
    }


    @OptIn(ExperimentalSerializationApi::class)
    private fun listThreads(usersIdentifiers: List<UserIdentifier>?): List<ConversationThreadOutputDTO> {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val request = usersIdentifiers?.let {
            RetrieveThreadDTO(
                byUsersPublicIdentifiers = it
            )
        }


        return "/threads/retrieve".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(request))
            .responseObject(
                ListSerializer(ConversationThreadOutputDTO.serializer()),
                Json {
                    explicitNulls = false
                }
            )
            .getOrThrow()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ): ConversationThreadOutputDTO {
        val bearerToken = this.requestor.authToken ?: throw HttpException(401, "unauthorized")

        val request = CreateOrUpdateThreadDTO(
            name = name,
            recipients = recipientsEncryptionDetails
        )
        val json = Json { explicitNulls = false }


        return "/threads/upsert".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(json.encodeToString(CreateOrUpdateThreadDTO.serializer(), request))
            .responseObject<ConversationThreadOutputDTO>(json)
            .getOrThrow()

    }

    override suspend fun upload(
        serverAsset: com.safehill.kclient.models.dtos.AssetOutputDTO,
        asset: EncryptedAsset,
        filterVersions: List<AssetQuality>,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
        asState: AssetDescriptorUploadState,
    ) {
        TODO("Not yet implemented")
    }

    @Throws
    override suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): List<AssetGlobalIdentifier> {
        val bearerToken = this.requestor.authToken ?: throw UnauthorizedSafehillHttpException

        val responseResult = "/assets/delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Gson().toJson(com.safehill.kclient.models.dtos.AssetDeleteCriteriaDTO(globalIdentifiers)))
            .response()
        return responseResult.getMappingOrThrow { globalIdentifiers }
    }

    override suspend fun setGroupEncryptionDetails(
        groupId: GroupId,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteGroup(groupId: GroupId) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): List<RecipientEncryptionDetailsDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun addReactions(
        reactions: List<UserReactionDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun removeReaction(reaction: UserReactionDTO, fromGroupId: GroupId) {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun retrieveInteractions(
        inGroupId: GroupId,
        per: Int,
        page: Int,
        before: String?
    ): InteractionsGroupDTO {
        val bearerToken =
            this.requestor.authToken ?: throw HttpException(
                401,
                "unauthorized"
            )

        val requestBody = GetInteractionDTO(
            per = per,
            page = page,
            referencedInteractionId = null,
            before = before
        )

        return "interactions/user-threads/$inGroupId".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(requestBody))
            .responseObject<InteractionsGroupDTO>(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
            .getOrThrow()

    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun addMessages(
        messages: List<MessageInputDTO>,
        groupId: GroupId
    ): List<MessageOutputDTO> {
        require(messages.size == 1) {
            "Can only add one message at a time."
        }
        val bearerToken =
            this.requestor.authToken ?: throw HttpException(
                401,
                "unauthorized"
            )

        return "interactions/user-threads/$groupId/messages".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(messages.first()))
            .responseObject<MessageOutputDTO>(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
            .getOrThrow().run(::listOf)
    }


    private fun <T, R> ResponseResultOf<T>.getMappingOrThrow(transform: (T) -> R): R {
        val value = getOrThrow()
        return transform(value)
    }

    private fun <T> ResponseResultOf<T>.getOrElseOnSafehillException(transform: (SafehillHttpException) -> T): T {
        return try {
            this.getOrThrow()
        } catch (e: Exception) {
            if (e is SafehillHttpException) {
                transform(e)
            } else {
                throw e
            }
        }
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
