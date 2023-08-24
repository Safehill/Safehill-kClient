package com.safehill.kcrypto.models

import java.math.BigInteger
import java.security.MessageDigest

class SHHash {

    companion object {

        fun stringDigest_legacyVersion(data: ByteArray): String {
            val sha512 = MessageDigest.getInstance("SHA-512")
            var hashText = BigInteger(1, sha512.digest(data)).toString(16)
            while (hashText.length < 32) {
                hashText = "0$hashText"
            }
            return hashText
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun stringDigest(data: ByteArray): String {
            val sha512 = MessageDigest.getInstance("SHA-512")
            sha512.update(data)
            return sha512.digest().toHexString()
        }
    }
}