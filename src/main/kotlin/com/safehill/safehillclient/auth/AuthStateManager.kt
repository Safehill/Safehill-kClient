package com.safehill.safehillclient.auth

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.model.auth.state.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AuthStateManager() {

    private val authState = MutableStateFlow<AuthState>(AuthState.SignedOff)

    private fun setAuthState(state: AuthState) {
        authState.update { state }
    }

    fun getAuthState(): StateFlow<AuthState> {
        return authState
    }

    fun setLoading() {
        setAuthState(AuthState.Loading)
    }

    fun setSignedOn(user: LocalUser) {
        setAuthState(AuthState.SignedOn(user = user))
    }

    fun setSignedOff() {
        setAuthState(AuthState.SignedOff)
    }

}