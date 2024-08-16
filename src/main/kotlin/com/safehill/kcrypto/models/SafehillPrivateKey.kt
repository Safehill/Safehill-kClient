package com.safehill.kclient.models

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec

class SafehillPrivateKey {

    companion object {
        fun from(bytes: ByteArray): PrivateKey {
            val kf = KeyFactory.getInstance("EC")
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(bytes)
            return kf.generatePrivate(privateKeySpec)
        }
    }
}