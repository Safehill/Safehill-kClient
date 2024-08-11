package com.safehill.kclient.models

import com.safehill.kclient.SafehillCypher

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