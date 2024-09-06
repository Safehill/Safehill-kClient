package com.safehill.kclient.network.api.authorization

import com.safehill.kclient.models.dtos.authorization.UserAuthorizationRequestDTO
import com.safehill.kclient.models.dtos.authorization.UserAuthorizationStatusDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.postRequestForObjectResponse
import com.safehill.kclient.network.api.postRequestForStringResponse

class AuthorizationApiImpl(
    override val requestor: LocalUser
) : AuthorizationApi, BaseApi {

    override suspend fun getAuthorizationStatus(): UserAuthorizationStatusDTO {
        return postRequestForObjectResponse<Unit, UserAuthorizationStatusDTO>(
            endPoint = "/users/authorization-status",
            request = null,
            authenticationRequired = true
        )
    }

    override suspend fun authorizeUsers(userIdentifiers: List<UserIdentifier>) {
        postRequestForStringResponse<UserAuthorizationRequestDTO>(
            endPoint = "/users/authorize",
            request = UserAuthorizationRequestDTO(
                userPublicIdentifiers = userIdentifiers
            ),
            authenticationRequired = true
        )
    }

    override suspend fun blockUsers(userIdentifiers: List<UserIdentifier>) {
        postRequestForStringResponse(
            endPoint = "/users/block",
            request = UserAuthorizationRequestDTO(
                userPublicIdentifiers = userIdentifiers
            ),
            authenticationRequired = true
        )
    }
}