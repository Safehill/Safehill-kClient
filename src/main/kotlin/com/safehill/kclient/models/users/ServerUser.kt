package com.safehill.kclient.models.users

import com.safehill.kcrypto.models.CryptoUser

interface ServerUser: CryptoUser {
    val identifier: String
    val name: String
}

