package com.safehill.kcrypto.models

import com.safehill.kcrypto.models.SHSymmetricKeySize.BITS_128
import com.safehill.kcrypto.models.SHSymmetricKeySize.BITS_192
import com.safehill.kcrypto.models.SHSymmetricKeySize.BITS_256
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

enum class SHSymmetricKeySize {
    BITS_128, BITS_192, BITS_256
}

class SHSymmetricKey {

    public val secretKeySpec: SecretKeySpec
    val size: SHSymmetricKeySize = BITS_256

    constructor(size: SHSymmetricKeySize = BITS_256) {
        val keySize = when (size) {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SHSymmetricKey

        return this.bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = secretKeySpec.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}

val SHSymmetricKey.bytes: ByteArray
    get() = secretKeySpec.encoded