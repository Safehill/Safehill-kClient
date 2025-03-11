package com.safehill.safehillclient.model.auth.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthStateHolder {

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOff)
    val authState = _authState.asStateFlow()

    fun setAuthState(state: AuthState) {
        _authState.update { state }
    }

}