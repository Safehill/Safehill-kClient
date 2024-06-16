package com.safehill.kclient.network.api.authorization

import com.safehill.kclient.models.dtos.UserAuthorizationStatusDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.postRequest

class AuthorizationApiImpl(
    override val requestor: LocalUser
) : AuthorizationApi {

    override suspend fun getAuthorizationStatus(): UserAuthorizationStatusDTO {
        return postRequest<Unit, UserAuthorizationStatusDTO>(
            endPoint = "/users/authorization-status",
            request = null,
            authenticationRequired = true
        )
    }

    override suspend fun authorizeUser() {
        TODO("Not yet implemented")
    }

    override suspend fun blockUser() {
        TODO("Not yet implemented")
    }
}