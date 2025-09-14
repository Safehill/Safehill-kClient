package com.safehill.safehillclient.auth

import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.users.LocalUser

class UserValidator() {

    fun validateUserData(user: LocalUser) {
        require(user.identifier.isNotBlank()) { "User identifier cannot be blank" }
        require(user.shUser.key.public.encoded.isNotEmpty()) { "User public key is required" }
        require(user.shUser.key.private.encoded.isNotEmpty()) { "User private key is required" }
        require(user.shUser.signature.public.encoded.isNotEmpty()) { "User signature public key is required" }
        require(user.shUser.signature.private.encoded.isNotEmpty()) { "User signature private key is required" }
    }

    fun validateAuthResponse(response: AuthResponseDTO) {
        require(response.bearerToken.isNotBlank()) { "Invalid bearer token received" }
        require(response.encryptionProtocolSalt.isNotBlank()) { "Invalid encryption salt received" }
        require(response.user.identifier.isNotBlank()) { "Invalid user identifier received" }
    }

    fun hasValidCachedCredentials(user: LocalUser): Boolean {
        return user.authToken != null &&
                user.authToken!!.isNotBlank() &&
                user.encryptionSalt.isNotEmpty()
    }
}