package com.safehill.safehillclient.data.user.api

import com.safehill.kclient.models.users.LocalUser

interface UserStorage {

    suspend fun getUser(): LocalUser?

    suspend fun storeUser(user: LocalUser)

    suspend fun clear()

}