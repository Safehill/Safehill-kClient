package com.safehill.kcrypto.models

import com.safehill.kcrypto.SHCypher

class SHEncryptedData(
    data: ByteArray,
    key: SHSymmetricKey
) {

    val privateSecret = key

    val encryptedData = SHCypher.encrypt(
        data, key
    )

    constructor(
        data: ByteArray
    ) : this(
        data,
        SHSymmetricKey()
    )


}