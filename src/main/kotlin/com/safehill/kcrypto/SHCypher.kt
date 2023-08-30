package com.safehill.kcrypto

import at.favre.lib.hkdf.HKDF
import com.safehill.kcrypto.models.SHPublicKey
import com.safehill.kcrypto.models.SHShareablePayload
import com.safehill.kcrypto.models.SHSignature
import com.safehill.kcrypto.models.SignatureVerificationError
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.util.*
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// https://stackoverflow.com/questions/59577317/ios-cryptokit-in-java/59658891#59658891
// https://stackoverflow.com/questions/61332076/cross-platform-aes-encryption-between-ios-and-kotlin-java-using-apples-cryptokit

class SHCypher {

    companion object {

        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16

        fun generateRandomIV(length: Int = GCM_IV_LENGTH): ByteArray {
            val sr = SecureRandom()
            val iv = ByteArray(length)
            sr.nextBytes(iv)
            return iv
        }

        fun encrypt(message: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
            // Get Cipher Instance
            val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")

            // Create SecretKeySpec
            val keySpec = SecretKeySpec(key, "AES")

            // Create GCMParameterSpec
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            // Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec)

            // Perform Encryption
            val cipherText = ByteArray(cipher.getOutputSize(message.size))
            var ctLength = cipher.update(message, 0, message.size, cipherText, 0)
            ctLength += cipher.doFinal(cipherText, ctLength)
            return cipherText
        }

        fun encrypt(
            message: ByteArray,
            receiverPublicKey: PublicKey,
            ephemeralKey: KeyPair,
            protocolSalt: ByteArray,
            iv: ByteArray,
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
            val pseudoRandomKey = hkdf.extract(protocolSalt, sharedSecretFromKeyAgreement)
            val derivedSymmetricKey = hkdf.expand(pseudoRandomKey, sharedInfo, 32)

            val cypher = encrypt(message, derivedSymmetricKey, iv)

            // Signs the message
            val signature = SHSignature.sign(
                cypher + ephemeralKey.public.encoded + receiverPublicKey.encoded,
                senderSignatureKey.private
            )

            return SHShareablePayload(ephemeralKey.public.encoded, cypher, signature, null)
        }

        fun decrypt(cipherText: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
            val nonOptionalIV: ByteArray
            val encryptedData: ByteArray

            if (iv == null) {
                //
                // If IV is null, we are expecting the IV to be the first 16 bits for the cipherText
                //
                val base64EncodedCypher = Base64.getEncoder().encodeToString(cipherText)
                if (base64EncodedCypher.length < 60) {
                    throw AEADBadTagException()
                }

                val ivBase64 = base64EncodedCypher.substring(0, GCM_TAG_LENGTH)
                val cipherTextBase64 = base64EncodedCypher.substring(GCM_TAG_LENGTH)
                nonOptionalIV = Base64.getDecoder().decode(ivBase64)
                encryptedData = Base64.getDecoder().decode(cipherTextBase64)
            } else {
                nonOptionalIV = iv
                encryptedData = cipherText
            }

            // Get Cipher Instance
            val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
            // Create SecretKeySpec
            val keySpec = SecretKeySpec(key, "AES")
            // Create GCMParameterSpec
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, nonOptionalIV)
            // Initialize Cipher for DECRYPT_MODE
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec)
            // Perform Decryption
            return cipher.doFinal(encryptedData)
        }

        fun decrypt(
            sealedMessage: SHShareablePayload,
            encryptionKey: KeyPair,
            protocolSalt: ByteArray,
            iv: ByteArray? = null,
            signedBy: PublicKey
        ): ByteArray {
            return this.decrypt(
                sealedMessage,
                encryptionKey.private,
                encryptionKey.public,
                protocolSalt,
                iv,
                signedBy
            )
        }

        fun decrypt(
            sealedMessage: SHShareablePayload,
            userPrivateKey: PrivateKey,
            userPublicKey: PublicKey,
            protocolSalt: ByteArray,
            iv: ByteArray? = null,
            signedBy: PublicKey
        ): ByteArray {
            // Verify the signature matches
            val data = sealedMessage.ciphertext + sealedMessage.ephemeralPublicKeyData + userPublicKey.encoded
            if (!SHSignature.verify(data, sealedMessage.signature, signedBy)) {
                throw SignatureVerificationError("Invalid signature")
            }

            // Retrieve the shared secret from the key agreement
            val ephemeralKey: PublicKey = SHPublicKey.from(sealedMessage.ephemeralPublicKeyData)
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(userPrivateKey)
            keyAgreement.doPhase(ephemeralKey, true)
            val sharedSecret = keyAgreement.generateSecret()

            val sharedInfo: ByteArray = ephemeralKey.encoded +
                    userPublicKey.encoded +
                    signedBy.encoded

            val hkdf = HKDF.fromHmacSha256()
            val pseudoRandomKey = hkdf.extract(protocolSalt, sharedSecret)
            val derivedSymmetricKey = hkdf.expand(pseudoRandomKey, sharedInfo, 32)

            return decrypt(sealedMessage.ciphertext, derivedSymmetricKey, iv)
        }
    }
}