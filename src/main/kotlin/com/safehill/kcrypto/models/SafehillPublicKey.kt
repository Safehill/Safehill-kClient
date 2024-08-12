package com.safehill.kclient.models

import java.math.BigInteger
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey

class SafehillPublicKey {
    companion object {
        fun from(bytes: ByteArray): PublicKey {
            val keyFactory = KeyFactory.getInstance("EC")
            return keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(bytes))
        }

        @Throws(java.lang.Exception::class)
        fun derivePublicKeyFrom(privateKey: PrivateKey): PublicKey {
            val ecPrivateKey = privateKey as java.security.interfaces.ECPrivateKey

            // Get the EC parameter spec from the private key
            val ecSpec: org.bouncycastle.jce.spec.ECParameterSpec =
                org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256r1")

            // Retrieve the private value s from the ECPrivateKey
            val s: BigInteger = ecPrivateKey.s

            // Calculate the public point W by multiplying the curve's generator point by s
            val q: org.bouncycastle.math.ec.ECPoint = ecSpec.g.multiply(s)

            // Create the public key specification
            val pubSpec = org.bouncycastle.jce.spec.ECPublicKeySpec(q, ecSpec)

            // Generate the public key
            val keyFactory = KeyFactory.getInstance("EC", "BC")
            return keyFactory.generatePublic(pubSpec) as java.security.interfaces.ECPublicKey
        }
    }
}