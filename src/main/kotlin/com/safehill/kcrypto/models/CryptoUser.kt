package com.safehill.kclient.models

import com.safehill.kclient.SafehillCypher
import java.security.KeyPair
import java.security.PublicKey


interface CryptoUser {
    val publicKey: PublicKey
    val publicSignature: PublicKey

    val publicKeyData: ByteArray
    val publicSignatureData: ByteArray
}

class RemoteCryptoUser(
    override val publicKeyData: ByteArray,
    override val publicSignatureData: ByteArray
) :
    CryptoUser {

    override val publicKey: PublicKey
        get() {
            return SafehillPublicKey.from(this.publicKeyData)
        }

    override val publicSignature: PublicKey
        get() {
            return SafehillPublicKey.from(this.publicSignatureData)
        }

    fun isValidSignature(signature: ByteArray, data: ByteArray): Boolean {
        return SafehillSignature.verify(data, signature, this.publicSignature)
    }

}

class LocalCryptoUser(val key: KeyPair, val signature: KeyPair) : CryptoUser {

    constructor() : this(SafehillKeyPair.generate(), SafehillKeyPair.generate())

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
            return SafehillHash.stringDigest(this.publicSignatureData)
        }

    fun sign(data: ByteArray): ByteArray {
        return SafehillSignature.sign(data, this.signature.private)
    }

}


class SHUserContext(private val user: LocalCryptoUser) {

    fun shareable(
        data: ByteArray,
        with: CryptoUser,
        protocolSalt: ByteArray
    ): ShareablePayload {

        val ephemeralKey = SafehillKeyPair.generate()
        val encrypted = SafehillCypher.encrypt(
            data,
            with.publicKey,
            ephemeralKey,
            protocolSalt,
            this.user.signature
        )
        return ShareablePayload(
            ephemeralKey.public.encoded,
            encrypted.ciphertext,
            encrypted.signature,
            with
        )
    }

    fun decrypt(
        data: ByteArray,
        encryptedSecret: ShareablePayload,
        protocolSalt: ByteArray,
        sender: CryptoUser
    ): ByteArray {
        val secretData = SafehillCypher.decrypt(
            encryptedSecret,
            this.user.key,
            protocolSalt,
            sender.publicSignature
        )
        return SafehillCypher.decrypt(data, secretData)
    }
}
