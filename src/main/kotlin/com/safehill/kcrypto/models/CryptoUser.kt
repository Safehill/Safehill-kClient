package com.safehill.kclient.models

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