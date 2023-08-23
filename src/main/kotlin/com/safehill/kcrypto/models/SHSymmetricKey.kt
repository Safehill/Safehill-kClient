package com.safehill.kcrypto.models

import com.safehill.kcrypto.models.SHSymmetricKeySize.*
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

enum class SHSymmetricKeySize {
    BITS_128, BITS_192, BITS_256
}

class SHSymmetricKey {

    public val secretKeySpec: SecretKeySpec
    val size: SHSymmetricKeySize = BITS_256

    constructor(size: SHSymmetricKeySize = BITS_256) {
        val keySize = when(size) {
            BITS_128 -> 128
            BITS_192 -> 192
            BITS_256 -> 256
        }
        val sr = SecureRandom()
        val randomKeyBytes = ByteArray(keySize / 8)
        sr.nextBytes(randomKeyBytes)
        this.secretKeySpec = SecretKeySpec(randomKeyBytes, "AES")
    }

    constructor(data: ByteArray) {
        this.secretKeySpec = SecretKeySpec(data, "AES")
    }
}