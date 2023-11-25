package com.safehill.kclient.api

import com.safehill.kclient.api.dtos.*
import com.safehill.kclient.models.*


typealias AssetGlobalIdentifier = String

interface SHSafehillAPI {

    var requestor: SHLocalUser

    // MARK: User Management

    /// Creates a new user given their credentials, their public key and public signature (store in the `requestor` object)
    /// - Parameters:
    ///   - name: the username
    /// - Returns:
    ///   - the user just created
    suspend fun createUser(name: String): SHServerUser

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
        medium: SHSendCodeToUserRequestDTO.Medium
    )

    /// Updates an existing user details or credentials
    /// - Parameters:
    ///   - name: the new username
    ///   - password: the new user password
    /// - Returns:
    ///   - the user just created
    suspend fun updateUser(name: String?): SHServerUser

    /// Delete the user making the request and all related assets, metadata and sharing information
    /// - Parameters:
    ///   - name: the username
    ///   - password: the password for authorization
    suspend fun deleteAccount(name: String, password: String)

    /// Delete the user making the request and all related assets, metadata and sharing information
    suspend fun deleteAccount()

    /// Logs the current user, aka the requestor
    /// - Parameters:
    ///   - name: the username
    /// - Returns:
    ///   - the response with the auth token if credentials are valid
    suspend fun signIn(name: String): SHAuthResponseDTO

    /// Get a User's public key and public signature
    /// - Parameters:
    ///   - userIdentifiers: the unique identifiers for the users. If NULL, retrieves all the connected users
    /// - Returns:
    ///   - the users matching the criteria
    suspend fun getUsers(withIdentifiers: List<String>): List<SHRemoteUser>

    /// Get a User's public key and public signature
    /// - Parameters:
    ///   - query: the query string
    /// - Returns:
    ///   - the users matching the identifiers
    suspend fun searchUsers(query: String): List<SHRemoteUser>

    /// Get the descriptors for all the assets the local user has access to
    suspend  fun getAssetDescriptors(): List<SHAssetDescriptor>

    /// Get the descriptors for some assets given their identifiers.
    /// Only descriptors whose assets th local user has access to can be retrieved.
    /// - Parameters:
    ///   - assetGlobalIdentifiers: the list of asset identifiers
    /// - Returns:
    ///   - the descriptor for the assets matching the criteria
    suspend fun getAssetDescriptors(assetGlobalIdentifiers: List<AssetGlobalIdentifier>): List<SHAssetDescriptor>

    /// Retrieve assets data and metadata
    /// - Parameters:
    ///   - withGlobalIdentifiers: filtering by global identifier
    ///   - versions: filtering by version
    /// - Returns:
    ///   - the encrypted assets from the server
    suspend fun getAssets(globalIdentifiers: List<AssetGlobalIdentifier>,
                          versions: List<SHAssetQuality>?): Map<AssetGlobalIdentifier, SHEncryptedAsset>

    // MARK: Assets Write

    /// Create encrypted assets and their versions on the server, and retrieves the presigned URL for the client to upload.
    /// - Parameters:
    ///   - assets: the encrypted data for each asset
    ///   - groupId: the group identifier used for the first upload
    ///   - filterVersions: because the input `SHEncryptedAsset`, optionally specify which versions to pick up from the `assets` object
    /// - Returns:
    ///   - the list of assets created
    suspend fun create(assets: List<SHEncryptedAsset>,
                       groupId: String,
                       filterVersions: List<SHAssetQuality>?): List<SHAssetOutputDTO>

    /// Shares one or more assets with a set of users
    /// - Parameters:
    ///   - asset: the asset to share, with references to asset id, version and user id to share with
    suspend fun share(asset: SHShareableEncryptedAsset)

    /// Unshares one asset (all of its versions) with a user. If the asset or the user don't exist, or the asset is not shared with the user, it's a no-op
    /// - Parameters:
    ///   - assetId: the identifier of asset previously shared
    ///   - with: the public identifier of the user it was previously shared with
    suspend fun unshare(
        assetId: AssetGlobalIdentifier,
        userPublicIdentifier: String
    )

    /// Upload encrypted asset versions data to the CDN.
    suspend fun upload(
        serverAsset: SHAssetOutputDTO,
        asset: SHEncryptedAsset,
        filterVersions: List<SHAssetQuality>
    )

    /// Mark encrypted asset versions data as uploaded to the CDN.
    /// - Parameters:
    ///   - assetGlobalIdentifier: the global identifier of the asset
    ///   - quality: the version of the asset
    ///   - as: the new state
    suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: SHAssetQuality,
        asState: SHAssetDescriptorUploadState
    )

    /// Removes assets from the CDN and on the server
    /// - Parameters:
    ///   - withGlobalIdentifiers: the global identifier
    /// - Returns:
    ///   - the list of global identifiers that have been deleted
    suspend fun deleteAssets(globalIdentifiers: List<String>): List<String>

    /// Creates a group and provides the encryption details for users in the group for E2EE.
    /// This method needs to be called every time a share (group) is created so that reactions and comments can be added to it.
    /// - Parameters:
    ///   - groupId: the group identifier
    ///   - recipientsEncryptionDetails: the encryption details for each reciepient
    suspend fun setGroupEncryptionDetails(
        groupId: String,
        recipientsEncryptionDetails: List<SHRecipientEncryptionDetailsDTO>
    )

    /// Delete a group, related messages and reactions, given its id
    /// - Parameters:
    ///   - groupId: the group identifier
    suspend fun deleteGroup(groupId: String)

    /// Retrieved the E2EE details for a group, if one exists
    /// - Parameters:
    ///   - groupId: the group identifier
    ///   - completionHandler: the callback method
    suspend fun retrieveGroupUserEncryptionDetails(
        groupId: String,
    ): List<SHRecipientEncryptionDetailsDTO>


    /// Adds reactions to a share (group)
    /// - Parameters:
    ///   - reactions: the reactions details
    ///   - groupId: the group identifier
    /// - Returns:
    ///   - the list of reactions added
    suspend fun addReactions(
        reactions: List<SHUserReaction>,
        toGroupId: String
    ): List<SHReactionOutputDTO>

    /// Removes a reaction to an asset or a message
    /// - Parameters:
    ///   - reaction: the reaction type and references to remove
    ///   - fromGroupId: the group the reaction belongs to
    suspend fun removeReaction(
        reaction: SHUserReaction,
        fromGroupId: String
    )

    /// Retrieves all the messages and reactions for a group id. Results are paginated and returned in reverse cronological order.
    /// - Parameters:
    ///   - groupId: the group identifier
    ///   - per: the number of items to retrieve
    ///   - page: the page number, because results are paginated
    /// - Returns:
    ///   - the list of interactions (reactions and messages) in the group
    suspend fun retrieveInteractions(
        inGroupId: String,
        per: Int,
        page: Int
    ): List<SHInteractionsGroupDTO>

    /// Adds a messages to a share (group)
    /// - Parameters:
    ///   - messages: the message details
    ///   - groupId: the group identifier
    /// - Returns:
    ///   - the list of messages created
    suspend fun addMessages(
        messages: List<SHMessageInputDTO>,
        toGroupId: String
    ): List<SHMessageOutputDTO>
}
