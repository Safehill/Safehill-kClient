package com.safehill.kclient.network

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorUploadState
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.InteractionsSummaryDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.ReactionInputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.SendCodeToUserRequestDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import java.util.Date

interface SafehillApi {

    var requestor: LocalUser

    // MARK: User Management

    /// Creates a new user given their credentials, their public key and public signature (store in the `requestor` object)
    /// - Parameters:
    ///   - name: the username
    /// - Returns:
    ///   - the user just created
    suspend fun createUser(name: String): ServerUser

    /// Send a code to a user to verify identity, via either phone or SMS
    /// - Parameters:
    ///   - countryCode: the recipient's phone country code
    ///   - phoneNumber: the recipient's phone number
    ///   - code: the code to send
    ///   - medium: the medium, either SMS or email
    suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SendCodeToUserRequestDTO.Medium
    )

    /// Updates an existing user details or credentials
    /// - Parameters:
    ///   - name: the new username
    ///   - phoneNumber: the new phone number
    ///   - email: the new email
    /// - Returns:
    ///   - the user just created
    suspend fun updateUser(name: String?, phoneNumber: String?, email: String?): ServerUser

    /// Delete the user making the request and all related assets, metadata and sharing information
    suspend fun deleteAccount()

    /// Logs the current user, aka the requestor
    /// - Parameters:
    ///   - name: the username
    /// - Returns:
    ///   - the response with the auth token if credentials are valid
    suspend fun signIn(): AuthResponseDTO

    /**
     * Register a device with the server for push notifications and sockets.
     * @param deviceId the device id for the device to register.
     * @param token the token on which FCM will push the notifications.
     */
    suspend fun registerDevice(
        deviceId: String,
        token: String,
    )

    /// Get a User's public key and public signature
    /// - Parameters:
    ///   - userIdentifiers: the unique identifiers for the users. If NULL, retrieves all the connected users
    /// - Returns:
    ///   - the users matching the criteria
    @Throws
    suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, RemoteUser>

    /**
     * Get a User's public key and public signature
     * @param hashedPhoneNumbers: list of hashed phone numbers to retrieve the users.
     * @return [Map] of matched users. [Map.Entry.key] is the phone number hash and [Map.Entry.value] is the corresponding user.
     */
    suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, RemoteUser>

    /// Get a User's public key and public signature
    /// - Parameters:
    ///   - query: the query string
    /// - Returns:
    ///   - the users matching the identifiers
    suspend fun searchUsers(query: String, per: Int, page: Int): List<RemoteUser>

    /**
     * Get the descriptors for all the assets the local user has access to
     * @param assetGlobalIdentifiers if not empty, retrieve only the provided asset gids
     * @param groupIds only returns descriptors for assets that are shared via the group ids, and return the group information only for the provided these group ids
     * @param after retrieve only the ones uploaded or shared after this date
     */
    suspend fun getAssetDescriptors(
        assetGlobalIdentifiers: List<AssetGlobalIdentifier>?,
        groupIds: List<GroupId>?,
        after: Date?
    ): List<AssetDescriptor>

    /**
     * Retrieve asset descriptor created or updated since the reference date
     * @param after retrieve only the ones uploaded or shared after this date
     */
    suspend fun getAssetDescriptors(after: Date?): List<AssetDescriptor>

    suspend fun getAssets(
        threadId: String
    ): ConversationThreadAssetsDTO


    /// Retrieve assets data and metadata
    /// - Parameters:
    ///   - withGlobalIdentifiers: filtering by global identifier
    ///   - versions: filtering by version
    /// - Returns:
    ///   - the encrypted assets from the server
    suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>?
    ): Map<AssetGlobalIdentifier, EncryptedAsset>

    // MARK: Assets Write

    /// Create encrypted assets and their versions on the server, and retrieves the presigned URL for the client to upload.
    /// - Parameters:
    ///   - assets: the encrypted data for each asset
    ///   - groupId: the group identifier used for the first upload
    ///   - filterVersions: because the input `SHEncryptedAsset`, optionally specify which versions to pick up from the `assets` object
    /// - Returns:
    ///   - the list of assets created
    suspend fun create(
        assets: List<EncryptedAsset>,
        groupId: GroupId,
        filterVersions: List<AssetQuality>?
    ): List<AssetOutputDTO>

    /// Shares one or more assets with a set of users
    /// - Parameters:
    ///   - asset: the asset to share, with references to asset id, version and user id to share with
    suspend fun share(asset: ShareableEncryptedAsset)

    /// Unshares one asset (all of its versions) with a user. If the asset or the user don't exist, or the asset is not shared with the user, it's a no-op
    /// - Parameters:
    ///   - assetId: the identifier of asset previously shared
    ///   - with: the public identifier of the user it was previously shared with
    suspend fun unshare(
        assetId: AssetGlobalIdentifier,
        userPublicIdentifier: UserIdentifier
    )

    suspend fun topLevelInteractionsSummary(): InteractionsSummaryDTO

    suspend fun retrieveThread(
        usersIdentifiers: List<UserIdentifier>
    ): ConversationThreadOutputDTO?

    suspend fun retrieveThread(
        threadId: String
    ): ConversationThreadOutputDTO?

    suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ): ConversationThreadOutputDTO

    /// Upload encrypted asset versions data to the CDN.
    suspend fun upload(
        serverAsset: AssetOutputDTO,
        asset: EncryptedAsset,
        filterVersions: List<AssetQuality>
    )

    /// Mark encrypted asset versions data as uploaded to the CDN.
    /// - Parameters:
    ///   - assetGlobalIdentifier: the global identifier of the asset
    ///   - quality: the version of the asset
    ///   - as: the new state
    suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
        asState: AssetDescriptorUploadState
    )

    /// Removes assets from the CDN and on the server
    /// - Parameters:
    ///   - withGlobalIdentifiers: the global identifier
    /// - Returns:
    ///   - the list of global identifiers that have been deleted
    suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): List<AssetGlobalIdentifier>

    /// Creates a group and provides the encryption details for users in the group for E2EE.
    /// This method needs to be called every time a share (group) is created so that reactions and comments can be added to it.
    /// - Parameters:
    ///   - groupId: the group identifier
    ///   - recipientsEncryptionDetails: the encryption details for each reciepient
    suspend fun setGroupEncryptionDetails(
        groupId: GroupId,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    )

    /// Delete a group, related messages and reactions, given its id
    /// - Parameters:
    ///   - groupId: the group identifier
    suspend fun deleteGroup(groupId: GroupId)

    /// Retrieved the E2EE details for a group, if one exists
    /// - Parameters:
    ///   - groupId: the group identifier
    ///   - completionHandler: the callback method
    suspend fun retrieveGroupUserEncryptionDetails(
        groupId: GroupId,
    ): List<RecipientEncryptionDetailsDTO>


    /// Adds reactions to a share (group)
    /// - Parameters:
    ///   - reactions: the reactions details
    ///   - groupId: the group identifier
    /// - Returns:
    ///   - the list of reactions added
    suspend fun addReactions(
        reactions: List<ReactionInputDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO>

    /// Removes a reaction to an asset or a message
    /// - Parameters:
    ///   - reaction: the reaction type and references to remove
    ///   - fromGroupId: the group the reaction belongs to
    suspend fun removeReaction(
        reaction: ReactionOutputDTO,
        fromGroupId: GroupId
    )

    /// Retrieves all the messages and reactions for a group id. Results are paginated and returned in reverse cronological order.
    /// - Parameters:
    ///   - groupId: the group identifier
    ///   - per: the number of items to retrieve
    ///   - page: the page number, because results are paginated
    /// - Returns:
    ///   - the list of interactions (reactions and messages) in the group
    suspend fun retrieveInteractions(
        inGroupId: GroupId,
        per: Int,
        page: Int,
        before: String?
    ): InteractionsGroupDTO

    /// Adds a messages to a share (group)
    /// - Parameters:
    ///   - messages: the message details
    ///   - groupId: the group identifier
    /// - Returns:
    ///   - the list of messages created
    suspend fun addMessages(
        messages: List<MessageInputDTO>,
        groupId: GroupId
    ): List<MessageOutputDTO>

    suspend fun listThreads(): List<ConversationThreadOutputDTO>
}
