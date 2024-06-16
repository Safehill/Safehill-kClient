package com.safehill.kclient.network.api.authorization

import com.safehill.kclient.models.dtos.UserAuthorizationStatusDTO
import com.safehill.kclient.network.api.BaseApi

interface AuthorizationApi : BaseApi {
    suspend fun getAuthorizationStatus(): UserAuthorizationStatusDTO

    suspend fun authorizeUser()

    suspend fun blockUser()
}