package com.safehill.kcrypto.models

import com.safehill.kcrypto.SafehillCypher

class EncryptedData(
    data: ByteArray,
    key: SymmetricKey
) {

    val privateSecret = key

    val encryptedData = SafehillCypher.encrypt(
        data, key
    )

    constructor(
        data: ByteArray
    ) : this(
        data,
        SymmetricKey()
    )


}