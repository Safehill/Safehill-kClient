package com.safehill.kclient.models.users

import com.safehill.kclient.errors.LocalUserError
import com.safehill.kclient.util.Provider

typealias UserProvider = Provider<LocalUser>

fun UserProvider.getOrNull() = try {
    this.get()
} catch (e: LocalUserError.UnAvailable) {
    null
}