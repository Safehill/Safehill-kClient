package com.safehill.safehillclient.manager.dependencies

import com.safehill.kclient.models.users.LocalUser

interface UserObserver {

    suspend fun userLoggedIn(user: LocalUser)

    fun userLoggedOut()

}