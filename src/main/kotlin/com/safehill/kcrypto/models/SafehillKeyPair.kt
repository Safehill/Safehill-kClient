package com.safehill.kclient.models

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec


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

