package com.safehill.kclient.models

import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

// https://metamug.com/article/security/sign-verify-digital-signature-ecdsa-java.html

class SignatureVerificationError(override val message: String) : Error(message) {
}

class SafehillSignature {

    companion object {
        fun sign(message: ByteArray, signaturePrivateKey: PrivateKey): ByteArray {
            val ecdsaSign = Signature.getInstance("SHA256withECDSA")
            ecdsaSign.initSign(signaturePrivateKey)
            ecdsaSign.update(message)
            return ecdsaSign.sign() ?: throw Error("unable to sign message")
        }

        fun verify(message: ByteArray, messageSignature: ByteArray, signaturePublicKey: PublicKey): Boolean {
            val ecdsaVerify = Signature.getInstance("SHA256withECDSA")
            ecdsaVerify.initVerify(signaturePublicKey)
            ecdsaVerify.update(message)
            return ecdsaVerify.verify(messageSignature)
        }
    }
}