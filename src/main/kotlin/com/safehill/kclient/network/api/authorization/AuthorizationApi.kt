package com.safehill.kclient.network.api.authorization

import com.safehill.kclient.models.dtos.authorization.UserAuthorizationStatusDTO
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.api.BaseApi

interface AuthorizationApi : BaseApi {
    suspend fun getAuthorizationStatus(): UserAuthorizationStatusDTO

    suspend fun authorizeUsers(userIdentifiers: List<UserIdentifier>)

    suspend fun blockUsers(userIdentifiers: List<UserIdentifier>)
}