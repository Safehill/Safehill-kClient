package com.safehill.kcrypto

import at.favre.lib.hkdf.HKDF
import com.safehill.kcrypto.models.SHPublicKey
import com.safehill.kcrypto.models.SHShareablePayload
import com.safehill.kcrypto.models.SHSignature
import com.safehill.kcrypto.models.SignatureVerificationError
import java.security.KeyPair
import java.security.PublicKey
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// https://stackoverflow.com/questions/59577317/ios-cryptokit-in-java/59658891#59658891
// https://stackoverflow.com/questions/61332076/cross-platform-aes-encryption-between-ios-and-kotlin-java-using-apples-cryptokit

class SHCypher {

    companion object {

        // TODO: Edit this. Should this be common across all clients, or unique per user?
        val STATIC_IV: ByteArray = Base64.getDecoder().decode("/5RWVwIP//+i///Z")
        // TODO: Hard-coded PROTOCOL SALT ??
        val PROTOCOL_SALT: ByteArray = Base64.getDecoder().decode("/5RWVwIP//+i///Z")

        val GCM_IV_LENGTH = 12
        val GCM_TAG_LENGTH = 16

        fun generateRandomIV(length: Int = GCM_IV_LENGTH): ByteArray {
            val sr = SecureRandom()
            val iv = ByteArray(length)
            sr.nextBytes(iv)
            return iv
        }

        fun encrypt(message: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
            // Get Cipher Instance
            val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")

            // Create SecretKeySpec
            val keySpec = SecretKeySpec(key, "AES")

            // Create GCMParameterSpec
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv ?: STATIC_IV)
            // Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec)

            // Perform Encryption
//            return cipher.doFinal(plaintext)
            val cipherText = ByteArray(cipher.getOutputSize(message.size))
            var ctLength = cipher.update(message, 0, message.size, cipherText, 0)
            ctLength += cipher.doFinal(cipherText, ctLength)
            return cipherText
        }

        fun decrypt(cipherText: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
            // Get Cipher Instance
            val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")

            // Create SecretKeySpec
            val keySpec = SecretKeySpec(key, "AES")

            // Create GCMParameterSpec
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv ?: STATIC_IV)
            // Initialize Cipher for DECRYPT_MODE
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec)

            // Perform Decryption
            val decryptedText = cipher.doFinal(cipherText)
            return decryptedText
        }

        fun encrypt(
            message: ByteArray,
            receiverPublicKey: PublicKey,
            ephemeralKey: KeyPair,
            senderSignatureKey: KeyPair
        ) : SHShareablePayload
        {
            // Generate a new private key (SHARED SECRET) from the key agreement
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(ephemeralKey.private)
            keyAgreement.doPhase(receiverPublicKey, true)
            val sharedSecretFromKeyAgreement = keyAgreement.generateSecret()

            // Information to share
            val sharedInfo: ByteArray = ephemeralKey.public.encoded +
                    receiverPublicKey.encoded +
                    senderSignatureKey.public.encoded

            // Derives a symmetric encryption key from the secret using HKDF key derivation
            val hkdf = HKDF.fromHmacSha256()
            val pseudoRandomKey = hkdf.extract(PROTOCOL_SALT, sharedSecretFromKeyAgreement)
            val derivedSymmetricKey = hkdf.expand(pseudoRandomKey, sharedInfo, 32)

            val cypher = encrypt(message, derivedSymmetricKey)

            // Signs the message
            val signature = SHSignature.sign(
                cypher + ephemeralKey.public.encoded + receiverPublicKey.encoded,
                senderSignatureKey.private
            )

            return SHShareablePayload(ephemeralKey.public.encoded, cypher, signature, null)
        }

        fun decrypt(
            sealedMessage: SHShareablePayload,
            encryptionKey: KeyPair,
            signedBy: PublicKey
        ): ByteArray {
            // Verify the signature matches
            val data = sealedMessage.ciphertext + sealedMessage.ephemeralPublicKeyData + encryptionKey.public.encoded
            if (!SHSignature.verify(data, sealedMessage.signature, signedBy)) {
                throw SignatureVerificationError("Invalid signature")
            }

            // Retrieve the shared secret from the key agreement
            val ephemeralKey: PublicKey = SHPublicKey.from(sealedMessage.ephemeralPublicKeyData)
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(encryptionKey.private)
            keyAgreement.doPhase(ephemeralKey, true)
            val sharedSecret = keyAgreement.generateSecret()

            val sharedInfo: ByteArray = ephemeralKey.encoded +
                    encryptionKey.public.encoded +
                    signedBy.encoded

            val hkdf = HKDF.fromHmacSha256()
            val pseudoRandomKey = hkdf.extract(PROTOCOL_SALT, sharedSecret)
            val derivedSymmetricKey = hkdf.expand(pseudoRandomKey, sharedInfo, 32)

            return decrypt(sealedMessage.ciphertext, derivedSymmetricKey)
        }
    }
}