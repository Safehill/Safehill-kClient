package com.safehill.safehillclient.auth

import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.api.auth.AuthApi
import com.safehill.safehillclient.model.SignInResponse

class OnlineAuthenticationStrategy(
    private val authApi: AuthApi,
    private val userValidator: UserValidator
) : AuthenticationStrategy {

    override suspend fun authenticate(user: LocalUser): SignInResponse.Success {
        val response = authApi.signIn(user = user)
        userValidator.validateAuthResponse(response)
        user.authenticate(response.user, response)

        return SignInResponse.Success(
            authResponseDTO = response,
            currentUser = user.updateFromServerUser(response.user)
        )
    }

    private fun LocalUser.updateFromServerUser(serverUser: ServerUser) = apply {
        this.name = serverUser.name
    }
}