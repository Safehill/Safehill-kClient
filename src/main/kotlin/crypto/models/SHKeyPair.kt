package crypto.models

import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


class SHKeyPair {
    companion object {
        fun generate(): KeyPair {
            val g = KeyPairGenerator.getInstance("EC")
            val spec = ECGenParameterSpec("secp256r1")
            g.initialize(spec, SecureRandom())
            return g.generateKeyPair()
        }
    }
}

class SHPrivateKey {

    companion object {
        fun from(bytes: ByteArray): PrivateKey {
            val kf = KeyFactory.getInstance("EC")
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(bytes)
            return kf.generatePrivate(privateKeySpec)
        }
    }
}

class SHPublicKey {
    companion object {
        fun from(bytes: ByteArray): PublicKey {
            val keyFactory = KeyFactory.getInstance("EC")
            return keyFactory.generatePublic(X509EncodedKeySpec(bytes))
        }
    }
}