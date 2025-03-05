package com.safehill.safehillclient.sdk.model

import com.safehill.kclient.models.dtos.AuthResponseDTO

sealed class SignInResponse {

    data class Success(
        val authResponseDTO: AuthResponseDTO
    ) : SignInResponse()

    /**
     * The sign in was a success but the responseDTO is unknown.
     * This usually happens if the user is already signed in and the SDK tries to sign in the same user.
     * Sign Out the current user and then sign in again to get the latest info.
     */
    data object SuccessWithUnknownMetadata : SignInResponse()

    val isPhoneNumberVerified: Boolean
        get() = this is Success && this.authResponseDTO.metadata.isPhoneNumberVerified

}