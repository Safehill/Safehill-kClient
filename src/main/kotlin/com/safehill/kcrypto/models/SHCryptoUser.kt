package com.safehill.kcrypto.models

import com.safehill.kcrypto.SHCypher
import java.security.KeyPair
import java.security.PublicKey


interface SHCryptoUser {
    val publicKey: PublicKey
    val publicSignature: PublicKey

    val publicKeyData: ByteArray
    val publicSignatureData: ByteArray
}

class SHRemoteCryptoUser(
    override val publicKeyData: ByteArray,
    override val publicSignatureData: ByteArray
) :
    SHCryptoUser {

    override val publicKey: PublicKey
        get() {
            return SHPublicKey.from(this.publicKeyData)
        }

    override val publicSignature: PublicKey
        get() {
            return SHPublicKey.from(this.publicSignatureData)
        }

    fun isValidSignature(signature: ByteArray, data: ByteArray): Boolean {
        return SHSignature.verify(data, signature, this.publicSignature)
    }

}

class SHLocalCryptoUser(val key: KeyPair, val signature: KeyPair) : SHCryptoUser {

    constructor() : this(SHKeyPair.generate(), SHKeyPair.generate())

    override val publicKey: PublicKey
        get() {
            return key.public
        }

    override val publicKeyData: ByteArray
        get() {
            return key.public.encoded
        }

    override val publicSignature: PublicKey
        get() {
            return signature.public
        }

    override val publicSignatureData: ByteArray
        get() {
            return signature.public.encoded
        }

    val identifier: String
        get() {
            return SHHash.stringDigest(this.publicSignatureData)
        }

    fun sign(data: ByteArray): ByteArray {
        return SHSignature.sign(data, this.signature.private)
    }

}


class SHUserContext(private val user: SHLocalCryptoUser) {

    fun shareable(
        data: ByteArray,
        with: SHCryptoUser,
        protocolSalt: ByteArray
    ): SHShareablePayload {

        val ephemeralKey = SHKeyPair.generate()
        val encrypted = SHCypher.encrypt(
            data,
            with.publicKey,
            ephemeralKey,
            protocolSalt,
            this.user.signature
        )
        return SHShareablePayload(
            ephemeralKey.public.encoded,
            encrypted.ciphertext,
            encrypted.signature,
            with
        )
    }

    fun decrypt(
        data: ByteArray,
        encryptedSecret: SHShareablePayload,
        protocolSalt: ByteArray,
        sender: SHCryptoUser
    ): ByteArray {
        val secretData = SHCypher.decrypt(
            encryptedSecret,
            this.user.key,
            protocolSalt,
            sender.publicSignature
        )
        return SHCypher.decrypt(data, secretData)
    }
}
