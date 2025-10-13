package com.safehill.safehillclient.auth

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.model.SignInResponse

class OfflineAuthenticationStrategy(
    private val userValidator: UserValidator
) : AuthenticationStrategy {

    override suspend fun authenticate(user: LocalUser): SignInResponse {
        if (!userValidator.hasValidCachedCredentials(user)) {
            throw IllegalStateException("Cannot authenticate offline.")
        }
        userValidator.validateUserData(user)
        return SignInResponse.SuccessWithUnknownMetadata(user)
    }
}