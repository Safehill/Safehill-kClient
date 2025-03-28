package com.safehill.safehillclient.model

import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.users.LocalUser

sealed class SignInResponse(
    open val currentUser: LocalUser
) {

    data class Success(
        val authResponseDTO: AuthResponseDTO,
        override val currentUser: LocalUser
    ) : SignInResponse(currentUser)

    /**
     * The sign in was a success but the responseDTO is unknown.
     * This usually happens if the user is already signed in and the SDK tries to sign in the same user.
     * Sign Out the current user and then sign in again to get the latest info.
     */
    data class SuccessWithUnknownMetadata(
        override val currentUser: LocalUser
    ) : SignInResponse(currentUser)

    val isPhoneNumberVerified: Boolean?
        get() = (this as? Success)?.authResponseDTO?.metadata?.isPhoneNumberVerified

}