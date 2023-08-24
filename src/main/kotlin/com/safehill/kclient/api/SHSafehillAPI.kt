package com.safehill.kclient.api

import com.safehill.kclient.api.dtos.SHAuthResponse
import com.safehill.kclient.models.SHLocalUser
import com.safehill.kclient.models.SHServerUser

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
    ///   - name: the user name
    ///   - password: the password for authorization
    suspend fun deleteAccount(name: String, password: String)

    /// Delete the user making the request and all related assets, metadata and sharing information
    suspend fun deleteAccount()

    /// Logs the current user, aka the requestor
    suspend fun signIn(name: String): SHAuthResponse

    /// Get a User's public key and public signature
    /// - Parameters:
    ///   - userIdentifiers: the unique identifiers for the users. If NULL, retrieves all the connected users
    suspend fun getUsers(withIdentifiers: Array<String>?): Array<SHServerUser>

    /// Get a User's public key and public signature
    /// - Parameters:
    ///   - query: the query string
    suspend fun searchUsers(query: String): Array<SHServerUser>

}
