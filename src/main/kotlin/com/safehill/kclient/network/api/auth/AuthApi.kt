package com.safehill.kclient.network.api.auth

import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import java.security.PublicKey

interface AuthApi {

    /**
     * Creates a new user given their credentials, their public key and public signature.
     * @param name: the username.
     * @param identifier: the unique identifier for user.
     * @param publicKey: the public key of the user.
     * @param signature: the signature of the user.
     * @return the user just created
     */
    suspend fun createUser(
        name: String,
        identifier: String,
        publicKey: PublicKey,
        signature: PublicKey
    ): ServerUser


    /**
     * Sign in the user.
     * @param user: the user to login.
     * @return
     * the response with the auth token if credentials are valid
     */
    suspend fun signIn(user: LocalUser): AuthResponseDTO

}