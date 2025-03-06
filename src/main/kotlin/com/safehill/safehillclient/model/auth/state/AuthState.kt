package com.safehill.safehillclient.model.auth.state

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.model.user.toAppUser

sealed class AuthState {
    data class SignedOn(val user: LocalUser) : AuthState() {
        val appUser = user.toAppUser()
    }

    data object SignedOff : AuthState()
    data object Loading : AuthState()
    data object NoUser : AuthState()

    fun isLoggedIn() = this is SignedOn
}