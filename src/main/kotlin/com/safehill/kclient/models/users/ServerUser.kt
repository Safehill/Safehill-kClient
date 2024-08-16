package com.safehill.kclient.models.users

import com.safehill.kclient.models.CryptoUser

interface ServerUser: CryptoUser {
    val identifier: UserIdentifier
    val name: String
}
