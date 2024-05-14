package com.safehill.kclient.models.users

import com.safehill.kcrypto.models.CryptoUser

interface ServerUser: CryptoUser {
    val identifier: UserIdentifier
    val name: String
}

