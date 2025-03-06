package com.safehill.safehillclient.sdk.model

import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.users.ServerUser

sealed class SignInResponse(
    open val serverUser: ServerUser
) {

    data class Success(
        val authResponseDTO: AuthResponseDTO
    ) : SignInResponse(authResponseDTO.user)

    /**
     * The sign in was a success but the responseDTO is unknown.
     * This usually happens if the user is already signed in and the SDK tries to sign in the same user.
     * Sign Out the current user and then sign in again to get the latest info.
     */
    class SuccessWithUnknownMetadata(
        serverUser: ServerUser
    ) : SignInResponse(serverUser)

    val isPhoneNumberVerified: Boolean
        get() = this is Success && this.authResponseDTO.metadata.isPhoneNumberVerified

}