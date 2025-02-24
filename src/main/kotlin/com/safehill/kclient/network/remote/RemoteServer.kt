package com.safehill.kclient.network.remote

import com.safehill.SafehillClient
import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.base64.base64EncodedString
import com.safehill.kclient.models.RemoteCryptoUser
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
import com.safehill.kclient.models.dtos.AssetShareDTO
import com.safehill.kclient.models.dtos.AuthChallengeRequestDTO
import com.safehill.kclient.models.dtos.AuthChallengeResponseDTO
import com.safehill.kclient.models.dtos.AuthResolvedChallengeDTO
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO
import com.safehill.kclient.models.dtos.FCM_TOKEN_TYPE
import com.safehill.kclient.models.dtos.GetInteractionDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.InteractionsSummaryDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.RemoteUserPhoneNumberMatchDto
import com.safehill.kclient.models.dtos.RemoteUserSearchDTO
import com.safehill.kclient.models.dtos.SendCodeToUserRequestDTO
import com.safehill.kclient.models.dtos.ShareVersionDetails
import com.safehill.kclient.models.dtos.UserDeviceTokenDTO
import com.safehill.kclient.models.dtos.UserIdentifiersDTO
import com.safehill.kclient.models.dtos.UserInputDTO
import com.safehill.kclient.models.dtos.UserPhoneNumbersDTO
import com.safehill.kclient.models.dtos.UserUpdateDTO
import com.safehill.kclient.models.dtos.toAssetDescriptor
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.SafehillApi
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.RequestMethod
import com.safehill.kclient.network.api.authorization.AuthorizationApi
import com.safehill.kclient.network.api.authorization.AuthorizationApiImpl
import com.safehill.kclient.network.api.fireRequestForObjectResponse
import com.safehill.kclient.network.api.group.GroupApi
import com.safehill.kclient.network.api.group.GroupApiImpl
import com.safehill.kclient.network.api.postRequestForObjectResponse
import com.safehill.kclient.network.api.postRequestForStringResponse
import com.safehill.kclient.network.api.reaction.ReactionApi
import com.safehill.kclient.network.api.reaction.ReactionApiImpl
import com.safehill.kclient.network.api.thread.ThreadApi
import com.safehill.kclient.network.api.thread.ThreadApiImpl
import com.safehill.kclient.network.remote.S3Proxy.Companion.fetchAssets
import com.safehill.kcrypto.models.ShareablePayload
import io.ktor.client.HttpClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64

class RemoteServer private constructor(
    private val baseApi: BaseApi
) : SafehillApi,
    AuthorizationApi by AuthorizationApiImpl(baseApi),
    GroupApi by GroupApiImpl(baseApi),
    ReactionApi by ReactionApiImpl(baseApi),
    ThreadApi by ThreadApiImpl(baseApi),
    BaseApi by baseApi {

    constructor(requestor: LocalUser, client: HttpClient) : this(
        object : BaseApi {
            override val requestor: LocalUser = requestor
            override val client: HttpClient = client
        }
    )

    @Throws
    override suspend fun createUser(name: String): ServerUser {
        val requestBody = UserInputDTO(
            identifier = requestor.identifier,
            publicKey = Base64.getEncoder().encodeToString(requestor.publicKeyData),
            publicSignature = Base64.getEncoder().encodeToString(requestor.publicSignatureData),
            name = name
        )
        return postRequestForObjectResponse<UserInputDTO, RemoteUser>(
            endPoint = "/users/create",
            request = requestBody,
            authenticationRequired = false
        )
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SendCodeToUserRequestDTO.Medium,
    ) {
        val requestBody = SendCodeToUserRequestDTO(
            countryCode = countryCode,
            phoneNumber = phoneNumber,
            code = code,
            medium = medium
        )
        postRequestForStringResponse(
            endPoint = "/users/code/send",
            request = requestBody,
            authenticationRequired = true
        )
    }

    @Throws
    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?,
    ): ServerUser {
        val requestBody = UserUpdateDTO(
            identifier = null,
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            publicKey = null,
            publicSignature = null
        )
        return postRequestForObjectResponse<UserUpdateDTO, RemoteUser>(
            endPoint = "/users/update",
            request = requestBody,
            authenticationRequired = true
        )
    }

    @Throws
    override suspend fun deleteAccount() {
        postRequestForStringResponse(
            endPoint = "/users/safe_delete",
            request = null,
            authenticationRequired = true
        )
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

        val authChallenge: AuthChallengeResponseDTO = postRequestForObjectResponse(
            endPoint = "/signin/challenge/start",
            request = authRequestBody,
            authenticationRequired = false
        )

        val solvedChallenge = this.solveChallenge(authChallenge)

        return postRequestForObjectResponse(
            endPoint = "/signin/challenge/verify",
            request = solvedChallenge,
            authenticationRequired = false
        )

    }

    override suspend fun registerDevice(deviceId: String, token: String?) {
        val userTokenRequest = UserDeviceTokenDTO(
            deviceId = deviceId,
            token = token,
            tokenType = FCM_TOKEN_TYPE
        )
        postRequestForStringResponse(
            endPoint = "/users/devices/register",
            request = userTokenRequest,
            authenticationRequired = true
        )
    }

    @Throws
    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, RemoteUser> {
        if (withIdentifiers.isEmpty()) {
            return emptyMap()
        }

        val getUsersRequestBody = UserIdentifiersDTO(userIdentifiers = withIdentifiers)
        return postRequestForObjectResponse<UserIdentifiersDTO, List<RemoteUser>>(
            endPoint = "/users/retrieve",
            request = getUsersRequestBody,
            authenticationRequired = true
        ).associateBy { it.identifier }
    }

    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, RemoteUser> {
        if (hashedPhoneNumbers.isEmpty()) {
            return mapOf()
        }
        val getUsersRequestBody = UserPhoneNumbersDTO(phoneNumbers = hashedPhoneNumbers)
        return postRequestForObjectResponse<UserPhoneNumbersDTO, RemoteUserPhoneNumberMatchDto>(
            endPoint = "/users/retrieve/phone-number",
            request = getUsersRequestBody,
            authenticationRequired = true
        ).result
    }

    @Throws
    override suspend fun searchUsers(query: String, per: Int, page: Int): List<RemoteUser> {
        return fireRequestForObjectResponse<List<Pair<String, String>>, RemoteUserSearchDTO>(
            requestMethod = RequestMethod.Get(
                query = listOf(
                    "query" to query,
                    "per" to per.toString(),
                    "page" to page.toString()
                )
            ),
            endPoint = "/users/search",
            request = null,
            authenticationRequired = true
        ).items
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
        return postRequestForObjectResponse<AssetDescriptorFilterCriteriaDTO, List<AssetDescriptorDTO>>(
            endPoint = "/assets/descriptors/retrieve",
            request = descriptorFilterCriteriaDTO,
            authenticationRequired = true
        ).map(AssetDescriptorDTO::toAssetDescriptor)
    }

    override suspend fun getAssets(threadId: String): ConversationThreadAssetsDTO {
        return postRequestForObjectResponse(
            endPoint = "/threads/retrieve/$threadId/assets",
            request = null,
            authenticationRequired = true
        )
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

        val assetOutputDTOs =
            postRequestForObjectResponse<AssetSearchCriteriaDTO, List<AssetOutputDTO>>(
                endPoint = "/assets/retrieve",
                request = assetFilterCriteriaDTO,
                authenticationRequired = true
            )
        return fetchAssets(assetOutputDTOs)
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
                    versionName = it.key.value,
                    senderEncryptedSecret = it.value.encryptedSecret.base64EncodedString(),
                    ephemeralPublicKey = it.value.publicKeyData.base64EncodedString(),
                    publicSignature = it.value.publicSignatureData.base64EncodedString(),
                )
            },
            forceUpdateVersions = true
        )
        val shOutput: AssetOutputDTO = postRequestForObjectResponse(
            endPoint = "/assets/create",
            request = requestBody,
            authenticationRequired = true
        )
        return listOf(shOutput)
    }

    override suspend fun share(asset: ShareableEncryptedAsset, threadId: String) {
        if (asset.sharedVersions.isEmpty() || asset.sharedVersions.size > 1) {
            throw NotImplementedError("Current API only supports share one asset per request")
        }


        val versions = mutableListOf<ShareVersionDetails>()
        for (version in asset.sharedVersions) {
            val versionDetails = ShareVersionDetails(
                versionName = version.quality.value,
                recipientUserIdentifier = version.userPublicIdentifier,
                recipientEncryptedSecret = Base64.getEncoder()
                    .encodeToString(version.encryptedSecret),
                ephemeralPublicKey = Base64.getEncoder().encodeToString(version.ephemeralPublicKey),
                publicSignature = Base64.getEncoder().encodeToString(version.publicSignature)
            )
            versions.add(versionDetails)
        }

        val requestBody = AssetShareDTO(
            globalAssetIdentifier = asset.globalIdentifier,
            versionSharingDetails = versions,  // Puoi avere più versioni in una lista
            groupId = asset.groupId,
            asPhotoMessageInThreadId = threadId
        )
        postRequestForStringResponse(
            endPoint = "/assets/share",
            request = requestBody,
            authenticationRequired = true
        )
    }

    override suspend fun unshare(
        assetId: AssetGlobalIdentifier,
        userPublicIdentifier: UserIdentifier
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun topLevelInteractionsSummary(): InteractionsSummaryDTO {
        return postRequestForObjectResponse(
            endPoint = "interactions/summary",
            request = null,
            authenticationRequired = true
        )
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
                        with(S3Proxy) {
                            upload(encryptedVersion.encryptedData, presignedURL)
                        }
                        remoteServer.markAsset(
                            asset.globalIdentifier,
                            encryptedVersion.quality,
                            AssetDescriptorUploadState.Completed
                        )
                    } catch (exception: Exception) {
                        SafehillClient.logger.error(
                            exception.message
                                ?: "Error while marking asset ${asset.globalIdentifier} ${encryptedVersion.quality}"
                        )
                        throw exception
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
        postRequestForStringResponse(
            endPoint = "assets/$assetGlobalIdentifier/versions/${quality.value}/uploaded",
            request = null,
            authenticationRequired = true
        )
    }

    @Throws
    override suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): List<AssetGlobalIdentifier> {
        postRequestForStringResponse(
            endPoint = "/assets/delete",
            request = AssetDeleteCriteriaDTO(
                globalIdentifiers
            ),
            authenticationRequired = true
        )
        return globalIdentifiers
    }

    override suspend fun setGroupEncryptionDetails(
        groupId: GroupId,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
    ) {
        TODO("Not yet implemented")
    }


    override suspend fun retrieveInteractions(
        anchorId: String,
        interactionAnchor: InteractionAnchor,
        per: Int,
        page: Int,
        before: String?
    ): InteractionsGroupDTO {
        val requestBody = GetInteractionDTO(
            per = per,
            page = page,
            referencedInteractionId = null,
            before = before
        )
        return postRequestForObjectResponse(
            endPoint = when (interactionAnchor) {
                InteractionAnchor.THREAD -> "interactions/user-threads/$anchorId"
                InteractionAnchor.GROUP -> "interactions/assets-groups/$anchorId"
            },
            request = requestBody,
            authenticationRequired = true
        )
    }

    override suspend fun addMessages(
        messages: List<MessageInputDTO>,
        anchorId: String,
        interactionAnchor: InteractionAnchor
    ): List<MessageOutputDTO> {
        require(messages.size == 1) {
            "Can only add one message at a time."
        }

        return postRequestForObjectResponse<MessageInputDTO, MessageOutputDTO>(
            endPoint = when (interactionAnchor) {
                InteractionAnchor.THREAD -> "interactions/user-threads/$anchorId/messages"
                InteractionAnchor.GROUP -> "interactions/assets-groups/$anchorId/messages"
            },
            request = messages.first(),
            authenticationRequired = true
        ).run(::listOf)
    }
}