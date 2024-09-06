package com.safehill.kclient.network.api.authorization

import com.safehill.kclient.models.dtos.authorization.UserAuthorizationStatusDTO
import com.safehill.kclient.models.users.UserIdentifier

interface AuthorizationApi {
    suspend fun getAuthorizationStatus(): UserAuthorizationStatusDTO

    suspend fun authorizeUsers(userIdentifiers: List<UserIdentifier>)

    suspend fun blockUsers(userIdentifiers: List<UserIdentifier>)
}