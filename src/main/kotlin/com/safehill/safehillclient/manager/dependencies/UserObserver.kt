package com.safehill.safehillclient.manager.dependencies

import com.safehill.kclient.models.users.LocalUser

interface UserObserver {

    fun userSet(user: LocalUser)

    fun clearUser(clearPersistence: Boolean)
}