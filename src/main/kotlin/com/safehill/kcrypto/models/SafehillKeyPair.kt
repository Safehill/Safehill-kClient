package com.safehill.kcrypto.models

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.PrivateKey
import java.security.PublicKey
import java.security.KeyFactory
import java.security.spec.ECGenParameterSpec
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class SafehillKeyPair {
    companion object {
        fun generate(): KeyPair {
            val g = KeyPairGenerator.getInstance("EC")
            val spec = ECGenParameterSpec("secp256r1")
            g.initialize(spec, SecureRandom())
            return g.generateKeyPair()
        }
    }
}

class SafehillPrivateKey {

    companion object {
        fun from(bytes: ByteArray): PrivateKey {
            val kf = KeyFactory.getInstance("EC")
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(bytes)
            return kf.generatePrivate(privateKeySpec)
        }
    }
}

class SafehillPublicKey {
    companion object {
        fun from(bytes: ByteArray): PublicKey {
            val keyFactory = KeyFactory.getInstance("EC")
            return keyFactory.generatePublic(X509EncodedKeySpec(bytes))
        }
    }
}