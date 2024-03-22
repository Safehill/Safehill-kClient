package com.safehill.kclient.models

import com.safehill.kcrypto.models.SHCryptoUser

interface SHServerUser: SHCryptoUser {
    val identifier: String
    val name: String
}

