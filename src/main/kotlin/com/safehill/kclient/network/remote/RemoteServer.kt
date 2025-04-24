package com.safehill.kclient.network.remote

import com.safehill.kclient.base64.base64EncodedString
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.dtos.AssetDeleteCriteriaDTO
import com.safehill.kclient.models.dtos.AssetDescriptorDTO
import com.safehill.kclient.models.dtos.AssetDescriptorFilterCriteriaDTO
import com.safehill.kclient.models.dtos.AssetShareDTO
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
import com.safehill.kclient.network.api.asset.AssetApi
import com.safehill.kclient.network.api.asset.AssetApiImpl
import com.safehill.kclient.network.api.authorization.AuthorizationApi
import com.safehill.kclient.network.api.authorization.AuthorizationApiImpl
import com.safehill.kclient.network.api.fireRequest
import com.safehill.kclient.network.api.group.GroupApi
import com.safehill.kclient.network.api.group.GroupApiImpl
import com.safehill.kclient.network.api.postRequest
import com.safehill.kclient.network.api.postRequestForResponse
import com.safehill.kclient.network.api.reaction.ReactionApi
import com.safehill.kclient.network.api.reaction.ReactionApiImpl
import com.safehill.kclient.network.api.thread.ThreadApi
import com.safehill.kclient.network.api.thread.ThreadApiImpl
import com.safehill.kclient.util.Provider
import io.ktor.client.HttpClient
import java.time.Instant

class RemoteServer private constructor(
    private val baseApi: BaseApi,
    private val safehillLogger: SafehillLogger
) : SafehillApi,
    AuthorizationApi by AuthorizationApiImpl(baseApi),
    GroupApi by GroupApiImpl(baseApi),
    ReactionApi by ReactionApiImpl(baseApi),
    ThreadApi by ThreadApiImpl(baseApi),
    BaseApi by baseApi,
    AssetApi by AssetApiImpl(baseApi, safehillLogger) {

    constructor(
        userProvider: Provider<LocalUser>,
        client: HttpClient,
        safehillLogger: SafehillLogger
    ) : this(
        baseApi = object : BaseApi {
            override val requestor: LocalUser
                get() = userProvider.get()
            override val client: HttpClient = client
        },
        safehillLogger = safehillLogger
    )

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
        postRequest(
            endPoint = "/users/code/send",
            request = requestBody
        )
    }

    @Throws
    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?,
        forcePhoneNumberLink: Boolean
    ): ServerUser {
        val requestBody = UserUpdateDTO(
            identifier = null,
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            publicKey = null,
            publicSignature = null,
            forcePhoneNumberLinking = forcePhoneNumberLink
        )
        return postRequestForResponse<UserUpdateDTO, RemoteUser>(
            endPoint = "/users/update",
            request = requestBody
        )
    }

    @Throws
    override suspend fun deleteAccount() {
        postRequest(
            endPoint = "/users/safe_delete",
            request = null
        )
    }

    override suspend fun registerDevice(deviceId: String, token: String?) {
        val userTokenRequest = UserDeviceTokenDTO(
            deviceId = deviceId,
            token = token,
            tokenType = FCM_TOKEN_TYPE
        )
        postRequest(
            endPoint = "/users/devices/register",
            request = userTokenRequest
        )
    }

    @Throws
    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, RemoteUser> {
        if (withIdentifiers.isEmpty()) {
            return emptyMap()
        }

        val getUsersRequestBody = UserIdentifiersDTO(userIdentifiers = withIdentifiers)
        return postRequestForResponse<UserIdentifiersDTO, List<RemoteUser>>(
            endPoint = "/users/retrieve",
            request = getUsersRequestBody
        ).associateBy { it.identifier }
    }

    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, RemoteUser> {
        if (hashedPhoneNumbers.isEmpty()) {
            return mapOf()
        }
        val getUsersRequestBody = UserPhoneNumbersDTO(phoneNumbers = hashedPhoneNumbers)
        return postRequestForResponse<UserPhoneNumbersDTO, RemoteUserPhoneNumberMatchDto>(
            endPoint = "/users/retrieve/phone-number",
            request = getUsersRequestBody
        ).result
    }

    @Throws
    override suspend fun searchUsers(query: String, per: Int, page: Int): List<RemoteUser> {
        return fireRequest<List<Pair<String, String>>, RemoteUserSearchDTO>(
            requestMethod = RequestMethod.Get(
                query = listOf(
                    "query" to query,
                    "per" to per.toString(),
                    "page" to page.toString()
                )
            ),
            endPoint = "/users/search",
            request = null
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
        return postRequestForResponse<AssetDescriptorFilterCriteriaDTO, List<AssetDescriptorDTO>>(
            endPoint = "/assets/descriptors/retrieve",
            request = descriptorFilterCriteriaDTO
        ).map(AssetDescriptorDTO::toAssetDescriptor)
    }

    override suspend fun getAssets(threadId: String): ConversationThreadAssetsDTO {
        return postRequestForResponse(
            endPoint = "/threads/retrieve/$threadId/assets",
            request = null
        )
    }


    override suspend fun share(asset: ShareableEncryptedAsset, threadId: String) {

        val versions = asset.sharedVersions.map { version ->
            ShareVersionDetails(
                versionName = version.quality.versionName,
                recipientUserIdentifier = version.userPublicIdentifier,
                recipientEncryptedSecret = version.encryptedSecret.base64EncodedString(),
                ephemeralPublicKey = version.ephemeralPublicKey.base64EncodedString(),
                publicSignature = version.publicSignature.base64EncodedString()
            )
        }

        val requestBody = AssetShareDTO(
            globalAssetIdentifier = asset.globalIdentifier,
            versionSharingDetails = versions,  // Puoi avere più versioni in una lista
            groupId = asset.groupId,
            asPhotoMessageInThreadId = threadId
        )
        postRequest(
            endPoint = "/assets/share",
            request = requestBody
        )
    }

    override suspend fun unshare(
        assetId: AssetGlobalIdentifier,
        userPublicIdentifier: UserIdentifier
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun topLevelInteractionsSummary(): InteractionsSummaryDTO {
        return postRequestForResponse(
            endPoint = "interactions/summary",
            request = null
        )
    }


    @Throws
    override suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): List<AssetGlobalIdentifier> {
        postRequest(
            endPoint = "/assets/delete",
            request = AssetDeleteCriteriaDTO(
                globalIdentifiers
            )
        )
        return globalIdentifiers
    }

    override suspend fun setGroupEncryptionDetails(
        groupId: GroupId,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
    ) {
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
        return postRequestForResponse(
            endPoint = when (interactionAnchor) {
                InteractionAnchor.THREAD -> "interactions/user-threads/$anchorId"
                InteractionAnchor.GROUP -> "interactions/assets-groups/$anchorId"
            },
            request = requestBody
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

        return postRequestForResponse<MessageInputDTO, MessageOutputDTO>(
            endPoint = when (interactionAnchor) {
                InteractionAnchor.THREAD -> "interactions/user-threads/$anchorId/messages"
                InteractionAnchor.GROUP -> "interactions/assets-groups/$anchorId/messages"
            },
            request = messages.first()
        ).run(::listOf)
    }
}