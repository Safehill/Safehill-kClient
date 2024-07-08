package com.safehill.kclient.network.api.authorization

import com.safehill.kclient.models.dtos.authorization.UserAuthorizationRequestDTO
import com.safehill.kclient.models.dtos.authorization.UserAuthorizationStatusDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.api.postForResponseObject
import com.safehill.kclient.network.api.postForResponseString

class AuthorizationApiImpl(
    override val requestor: LocalUser
) : AuthorizationApi {

    override suspend fun getAuthorizationStatus(): UserAuthorizationStatusDTO {
        return postForResponseObject<Unit, UserAuthorizationStatusDTO>(
            endPoint = "/users/authorization-status",
            request = null,
            authenticationRequired = true
        )
    }

    override suspend fun authorizeUsers(userIdentifiers: List<UserIdentifier>) {
        postForResponseString<UserAuthorizationRequestDTO>(
            endPoint = "/users/authorize",
            request = UserAuthorizationRequestDTO(
                userPublicIdentifiers = userIdentifiers
            ),
            authenticationRequired = true
        )
    }

    override suspend fun blockUsers(userIdentifiers: List<UserIdentifier>) {
        postForResponseString(
            endPoint = "/users/block",
            request = UserAuthorizationRequestDTO(
                userPublicIdentifiers = userIdentifiers
            ),
            authenticationRequired = true
        )
    }
}