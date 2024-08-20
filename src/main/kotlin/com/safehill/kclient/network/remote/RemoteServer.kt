package com.safehill.kclient.network.remote

import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.serialization.responseObject
import com.google.gson.Gson
import com.safehill.SafehillClient
import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.models.RemoteCryptoUser
import com.safehill.kclient.models.ShareablePayload
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorUploadState
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.dtos.AssetDeleteCriteriaDTO
import com.safehill.kclient.models.dtos.AssetDescriptorDTO
import com.safehill.kclient.models.dtos.AssetDescriptorFilterCriteriaDTO
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AssetSearchCriteriaDTO
import com.safehill.kclient.models.dtos.AuthChallengeRequestDTO
import com.safehill.kclient.models.dtos.AuthChallengeResponseDTO
import com.safehill.kclient.models.dtos.AuthResolvedChallengeDTO
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.CreateOrUpdateThreadDTO
import com.safehill.kclient.models.dtos.FCM_TOKEN_TYPE
import com.safehill.kclient.models.dtos.GetInteractionDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.InteractionsSummaryDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.ReactionInputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.RemoteUserPhoneNumberMatchDto
import com.safehill.kclient.models.dtos.RemoteUserSearchDTO
import com.safehill.kclient.models.dtos.RetrieveThreadDTO
import com.safehill.kclient.models.dtos.SendCodeToUserRequestDTO
import com.safehill.kclient.models.dtos.UserDeviceTokenDTO
import com.safehill.kclient.models.dtos.UserIdentifiersDTO
import com.safehill.kclient.models.dtos.UserInputDTO
import com.safehill.kclient.models.dtos.UserPhoneNumbersDTO
import com.safehill.kclient.models.dtos.UserUpdateDTO
import com.safehill.kclient.models.dtos.toAssetDescriptor
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.SafehillApi
import com.safehill.kclient.network.api.authorization.AuthorizationApi
import com.safehill.kclient.network.api.authorization.AuthorizationApiImpl
import com.safehill.kclient.network.api.getMappingOrThrow
import com.safehill.kclient.network.api.getOrElseOnSafehillError
import com.safehill.kclient.network.api.getOrThrow
import com.safehill.kclient.network.api.postForResponseObject
import com.safehill.kclient.network.exceptions.SafehillError
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64


// For Fuel how to see https://www.baeldung.com/kotlin/fuel

class RemoteServer(
    override val requestor: LocalUser
) : SafehillApi,
    AuthorizationApi by AuthorizationApiImpl(requestor) {

    @OptIn(ExperimentalSerializationApi::class)
    private val ignorantJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
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
            .body(Json.encodeToString(requestBody))
            .responseObject<RemoteUser>(ignorantJson)
            .getOrThrow()
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SendCodeToUserRequestDTO.Medium,
    ) {
        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        val requestBody = SendCodeToUserRequestDTO(
            countryCode = countryCode,
            phoneNumber = phoneNumber,
            code = code,
            medium = medium
        )

        "/users/code/send".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(requestBody))
            .response()
            .getOrThrow()

    }

    @Throws
    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?,
    ): ServerUser {
        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

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
            .body(Json.encodeToString(requestBody))
            .responseObject<RemoteUser>()
            .getOrThrow()
    }

    @Throws
    override suspend fun deleteAccount() {
        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

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

    override suspend fun registerDevice(deviceId: String, token: String) {
        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        val userTokenRequest = UserDeviceTokenDTO(
            deviceId = deviceId,
            token = token,
            tokenType = FCM_TOKEN_TYPE
        )
        "/users/devices/register".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(userTokenRequest))
            .responseString()
            .getOrThrow()
    }

    @Throws
    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, RemoteUser> {
        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

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
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

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
        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

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
    override suspend fun getAssetDescriptors(after: Instant?): List<AssetDescriptor> {
        return getAssetDescriptors(
            assetGlobalIdentifiers = null,
            groupIds = null,
            after = after
        )
    }

    @Throws
    override suspend fun getAssetDescriptors(
        assetGlobalIdentifiers: List<AssetGlobalIdentifier>?,
        groupIds: List<GroupId>?,
        after: Instant?
    ): List<AssetDescriptor> {
        val descriptorFilterCriteriaDTO = AssetDescriptorFilterCriteriaDTO(
            after = after?.toString(),
            globalIdentifiers = assetGlobalIdentifiers,
            groupIds = groupIds
        )
        return postForResponseObject<AssetDescriptorFilterCriteriaDTO, List<AssetDescriptorDTO>>(
            endPoint = "/assets/descriptors/retrieve",
            request = descriptorFilterCriteriaDTO,
            authenticationRequired = true
        ).map(AssetDescriptorDTO::toAssetDescriptor)
    }

    override suspend fun getAssets(threadId: String): ConversationThreadAssetsDTO {
        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        return "/threads/retrieve/$threadId/assets".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseObject(ConversationThreadAssetsDTO.serializer())
            .getOrThrow()
    }

    @Throws
    override suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>,
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        val assetFilterCriteriaDTO = AssetSearchCriteriaDTO(
            globalIdentifiers = globalIdentifiers,
            versionNames = versions.map { it.value }
        )

        val assetOutputDTOs = postForResponseObject<AssetSearchCriteriaDTO, List<AssetOutputDTO>>(
            endPoint = "/assets/retrieve",
            request = assetFilterCriteriaDTO,
            authenticationRequired = true
        )
        return S3Proxy.fetchAssets(assetOutputDTOs)

    }

    @Throws
    override suspend fun create(
        assets: List<EncryptedAsset>,
        groupId: GroupId,
        filterVersions: List<AssetQuality>?,
    ): List<AssetOutputDTO> {
        if (assets.size > 1) {
            throw NotImplementedError("Current API only supports creating one asset per request")
        }
        val asset = assets.first()

        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        val assetCreatedAt = asset.creationDate ?: run { Instant.MIN }
        val dateTime = OffsetDateTime.ofInstant(assetCreatedAt, ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
        val requestBody = com.safehill.kclient.models.dtos.AssetInputDTO(
            asset.globalIdentifier,
            asset.localIdentifier,
            dateTime.format(formatter),
            groupId,
            asset.encryptedVersions.map {
                com.safehill.kclient.models.dtos.AssetVersionInputDTO(
                    it.key.value,
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
            .responseObject(AssetOutputDTO.serializer())
            .getOrThrow()
        return listOf(shOutput)
    }

    override suspend fun share(asset: ShareableEncryptedAsset) {
        TODO("Not yet implemented")
    }

    override suspend fun unshare(
        assetId: AssetGlobalIdentifier,
        userPublicIdentifier: UserIdentifier
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun topLevelInteractionsSummary(): InteractionsSummaryDTO {
        val bearerToken = this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        return "interactions/summary".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseObject<InteractionsSummaryDTO>(json = ignorantJson)
            .getOrThrow()
    }

    override suspend fun retrieveThread(
        usersIdentifiers: List<UserIdentifier>,
        phoneNumbers: List<HashedPhoneNumber>
    ): ConversationThreadOutputDTO? {
        return listThreads(
            RetrieveThreadDTO(
                byUsersPublicIdentifiers = usersIdentifiers,
                byInvitedPhoneNumbers = phoneNumbers
            )
        ).firstOrNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        val bearerToken = this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        return "/threads/retrieve/$threadId".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseObject(
                ConversationThreadOutputDTO.serializer(),
                Json {
                    explicitNulls = false
                    ignoreUnknownKeys = true
                }
            )
            .getOrElseOnSafehillError {
                if (it is SafehillError.ClientError.NotFound) {
                    null
                } else {
                    throw it
                }
            }
    }


    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return listThreads(null)
    }

    private suspend fun listThreads(retrieveThreadDTO: RetrieveThreadDTO?): List<ConversationThreadOutputDTO> {
        return postForResponseObject<RetrieveThreadDTO, List<ConversationThreadOutputDTO>>(
            endPoint = "/threads/retrieve",
            request = retrieveThreadDTO,
            authenticationRequired = true
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ): ConversationThreadOutputDTO {
        val bearerToken = this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        val request = CreateOrUpdateThreadDTO(
            name = name,
            recipients = recipientsEncryptionDetails
        )
        val json = Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }


        return "/threads/upsert".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(json.encodeToString(CreateOrUpdateThreadDTO.serializer(), request))
            .responseObject<ConversationThreadOutputDTO>(json)
            .getOrThrow()

    }

    override suspend fun upload(
        serverAsset: AssetOutputDTO,
        asset: EncryptedAsset,
        filterVersions: List<AssetQuality>,
    ) {
        val encryptedVersionByPresignedURL =
            asset.encryptedVersions.values.filter { filterVersions.contains(it.quality) }
                .mapNotNull { encryptedVersion ->
                    try {
                        val serverAssetVersion = serverAsset.versions.first {
                            it.versionName == encryptedVersion.quality.value
                        }
                        serverAssetVersion.presignedURL to encryptedVersion
                    } catch (_: NoSuchElementException) {
                        SafehillClient.logger.log(
                            "no server asset provided for version ${encryptedVersion.quality.value}"
                        )
                        null
                    }
                }.toMap()

        val remoteServer = this
        coroutineScope {
            encryptedVersionByPresignedURL.map { (presignedURL, encryptedVersion) ->
                launch {
                    try {
                        S3Proxy.upload(encryptedVersion.encryptedData, presignedURL)
                        remoteServer.markAsset(
                            asset.globalIdentifier,
                            encryptedVersion.quality,
                            AssetDescriptorUploadState.Completed
                        )
                    } catch (exception: Exception) {
                        SafehillClient.logger.error(
                            exception.message ?: "Error while marking asset ${asset.globalIdentifier} ${encryptedVersion.quality}"
                        )
                    }
                }
            }
        }
    }

    override suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
        asState: AssetDescriptorUploadState,
    ) {
        val bearerToken =
            this.requestor.authToken ?: throw HttpException(
                401,
                "unauthorized"
            )

        "assets/$assetGlobalIdentifier/versions/${quality.value}/uploaded".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .response()
            .getOrThrow()
    }

    @Throws
    override suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): List<AssetGlobalIdentifier> {
        val bearerToken =
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        val responseResult = "/assets/delete".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(
                Gson().toJson(
                    AssetDeleteCriteriaDTO(
                        globalIdentifiers
                    )
                )
            )
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
        reactions: List<ReactionInputDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun removeReaction(reaction: ReactionOutputDTO, fromGroupId: GroupId) {
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
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

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
            this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        return "interactions/user-threads/$groupId/messages".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(Json.encodeToString(messages.first()))
            .responseObject<MessageOutputDTO>(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
            .getOrThrow()
            .run(::listOf)
    }

}