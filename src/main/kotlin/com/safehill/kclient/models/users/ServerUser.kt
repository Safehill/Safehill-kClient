package com.safehill.kclient.models.users

import com.safehill.kcrypto.models.SHCryptoUser

interface ServerUser: SHCryptoUser {
    val identifier: String
    val name: String
}

