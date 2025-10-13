package com.safehill.safehillclient.auth

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.model.SignInResponse

interface AuthenticationStrategy {
    suspend fun authenticate(user: LocalUser): SignInResponse
}