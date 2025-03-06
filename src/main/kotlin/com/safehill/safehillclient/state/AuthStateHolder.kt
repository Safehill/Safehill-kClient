package com.safehill.safehillclient.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class AuthStateHolder {


    private val _authState = MutableStateFlow<AuthState>(AuthState.NoUser)
    val authState = _authState.asStateFlow()

    val authenticatedUser = _authState.map { (it as? AuthState.SignedOn)?.user }

    fun setAuthState(state: AuthState) {
        _authState.update { state }
    }

}