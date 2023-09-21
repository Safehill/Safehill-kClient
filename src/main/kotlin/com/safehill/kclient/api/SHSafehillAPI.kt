package com.safehill.kclient.api

import com.safehill.kclient.api.dtos.SHAuthResponse
import com.safehill.kclient.api.dtos.SHServerAsset
import com.safehill.kclient.models.*


typealias AssetGlobalIdentifier = String

interface SHSafehillAPI {

    var requestor: SHLocalUser

    // MARK: User Management

    /// Creates a new user given their credentials, their public key and public signature (store in the `requestor` object)
    /// - Parameters:
    ///   - name  the username
    suspend fun createUser(name: String): SHServerUser

    /// Updates an existing user details or credentials
    /// - Parameters:
    ///   - name  the new username
    ///   - password  the new user password
    suspend fun updateUser(name: String?): SHServerUser

    /// Delete the user making the request and all related assets, metadata and sharing information
    /// - Parameters:
    ///   - name: the username
    ///   - password: the password for authorization
    suspend fun deleteAccount(name: String, password: String)

    /// Delete the user making the request and all related assets, metadata and sharing information
    suspend fun deleteAccount()

    /// Logs the current user, aka the requestor
    suspend fun signIn(name: String): SHAuthResponse

    /// Get a User's public key and public signature
    /// - Parameters:
    ///   - userIdentifiers: the unique identifiers for the users. If NULL, retrieves all the connected users
    suspend fun getUsers(withIdentifiers: List<String>): List<SHRemoteUser>

    /// Get a User's public key and public signature
    /// - Parameters:
    ///   - query: the query string
    suspend fun searchUsers(query: String): List<SHRemoteUser>

    /// Get the descriptors for all the assets the local user has access to
    suspend  fun getAssetDescriptors(): List<SHAssetDescriptor>

    /// Get the descriptors for some assets given their identifiers.
    /// Only descriptors whose assets th local user has access to can be retrieved.
    /// - Parameters:
    ///   - assetGlobalIdentifiers: the list of asset identifiers
    suspend fun getAssetDescriptors(assetGlobalIdentifiers: List<AssetGlobalIdentifier>): List<SHAssetDescriptor>

    /// Retrieve assets data and metadata
    /// - Parameters:
    ///   - withGlobalIdentifiers: filtering by global identifier
    ///   - versions: filtering by version
    ///   - completionHandler: the callback method
    suspend fun getAssets(withGlobalIdentifiers: List<String>,
                          versions: List<SHAssetQuality>?): Map<String, SHEncryptedAsset>

    // MARK: Assets Write

    /// Create encrypted asset and versions (low res and hi-res)
    /// - Parameters:
    ///   - assets: the encrypted data for each asset
    ///   - groupId: the group identifier used for the first upload
    ///   - completionHandler: the callback method
    suspend fun create(assets: List<SHEncryptedAsset>,
                       groupId: String,
                       filterVersions: List<SHAssetQuality>?): List<SHServerAsset>
}
