package com.safehill.kclient.network.api.authorization

import com.safehill.kclient.models.dtos.authorization.UserAuthorizationRequestDTO
import com.safehill.kclient.models.dtos.authorization.UserAuthorizationStatusDTO
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.postRequestForObjectResponse
import com.safehill.kclient.network.api.postRequestForStringResponse

class AuthorizationApiImpl(
    baseApi: BaseApi
) : AuthorizationApi, BaseApi by baseApi {

    override suspend fun getAuthorizationStatus(): UserAuthorizationStatusDTO {
        return postRequestForObjectResponse<Unit, UserAuthorizationStatusDTO>(
            endPoint = "/users/authorization-status",
            request = null
        )
    }

    override suspend fun authorizeUsers(userIdentifiers: List<UserIdentifier>) {
        postRequestForStringResponse<UserAuthorizationRequestDTO>(
            endPoint = "/users/authorize",
            request = UserAuthorizationRequestDTO(
                userPublicIdentifiers = userIdentifiers
            )
        )
    }

    override suspend fun blockUsers(userIdentifiers: List<UserIdentifier>) {
        postRequestForStringResponse(
            endPoint = "/users/block",
            request = UserAuthorizationRequestDTO(
                userPublicIdentifiers = userIdentifiers
            )
        )
    }
}