package com.safehill.kclient

import at.favre.lib.hkdf.HKDF
import com.safehill.kclient.models.SafehillPublicKey
import com.safehill.kclient.models.SafehillSignature
import com.safehill.kclient.models.ShareablePayload
import com.safehill.kclient.models.SignatureVerificationError
import com.safehill.kclient.models.SymmetricKey
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// https://stackoverflow.com/questions/59577317/ios-cryptokit-in-java/59658891#59658891
// https://stackoverflow.com/questions/61332076/cross-platform-aes-encryption-between-ios-and-kotlin-java-using-apples-cryptokit

class SafehillCypher {

    companion object {

        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16

        fun generateRandomIV(length: Int = GCM_IV_LENGTH): ByteArray {
            val sr = SecureRandom()
            val iv = ByteArray(length)
            sr.nextBytes(iv)
            return iv
        }




        fun encrypt(message: ByteArray, key: SymmetricKey, iv: ByteArray? = null): ByteArray {
            return this.encrypt(message, key.secretKeySpec.encoded, iv)
        }

        // Similar to swift in which iv is prepended at the beginning
        fun encrypt(message: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
            // Get Cipher Instance
            val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")

            val finalIV = iv ?: generateRandomIV()

            // Create SecretKeySpec
            val keySpec = SecretKeySpec(key, "AES")

            // Create GCMParameterSpec
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, finalIV)
            // Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec)

            // Perform Encryption
            val cipherText = ByteArray(cipher.getOutputSize(message.size))
            var ctLength = cipher.update(message, 0, message.size, cipherText, 0)
            ctLength += cipher.doFinal(cipherText, ctLength)
            return finalIV + cipherText
        }

        fun encrypt(
            message: ByteArray,
            receiverPublicKey: PublicKey,
            ephemeralKey: KeyPair,
            protocolSalt: ByteArray,
            senderSignatureKey: KeyPair
        ): ShareablePayload {
            // Generate a new private key (SHARED SECRET) from the key agreement

            val sharedSecretFromKeyAgreement = generatedSharedSecret(
                otherUserPublicKey = receiverPublicKey,
                selfPrivateKey = ephemeralKey.private
            )

            // Information to share
            val sharedInfo: ByteArray = ephemeralKey.public.encoded +
                    receiverPublicKey.encoded +
                    senderSignatureKey.public.encoded

            // Derives a symmetric encryption key from the secret using HKDF key derivation
            val hkdf = HKDF.fromHmacSha256()
            val pseudoRandomKey = hkdf.extract(protocolSalt, sharedSecretFromKeyAgreement)
            val derivedSymmetricKey = hkdf.expand(pseudoRandomKey, sharedInfo, 32)

            val cypher = encrypt(message, derivedSymmetricKey)

            // Signs the message
            val signature = SafehillSignature.sign(
                cypher + ephemeralKey.public.encoded + receiverPublicKey.encoded,
                senderSignatureKey.private
            )

            return ShareablePayload(ephemeralKey.public.encoded, cypher, signature, null)
        }

        fun decrypt(cipherText: ByteArray, key: SymmetricKey, iv: ByteArray? = null) =
            this.decrypt(cipherText, key.secretKeySpec.encoded, iv)

        fun decrypt(cipherText: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
            val nonOptionalIV: ByteArray
            val encryptedData: ByteArray

            if (iv == null) {
                //
                // If IV is null, we are expecting the IV to be the first 16 bits for the cipherText
                //
                val base64EncodedCypher = Base64.getEncoder().encodeToString(cipherText)

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
            sealedMessage: ShareablePayload,
            encryptionKey: KeyPair,
            protocolSalt: ByteArray,
            signedBy: PublicKey,
            iv: ByteArray? = null,
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
            sealedMessage: ShareablePayload,
            userPrivateKey: PrivateKey,
            userPublicKey: PublicKey,
            protocolSalt: ByteArray,
            iv: ByteArray? = null,
            signedBy: PublicKey
        ): ByteArray {
            // Verify the signature matches
            val data =
                sealedMessage.ciphertext + sealedMessage.ephemeralPublicKeyData + userPublicKey.encoded
            if (!SafehillSignature.verify(data, sealedMessage.signature, signedBy)) {
                throw SignatureVerificationError("Invalid signature")
            }

            // Retrieve the shared secret from the key agreement
            val ephemeralKey: PublicKey =
                SafehillPublicKey.from(sealedMessage.ephemeralPublicKeyData)

            val sharedSecret = generatedSharedSecret(
                otherUserPublicKey = ephemeralKey,
                selfPrivateKey = userPrivateKey
            )

            val sharedInfo: ByteArray = ephemeralKey.encoded +
                    userPublicKey.encoded +
                    signedBy.encoded

            val hkdf = HKDF.fromHmacSha256()
            val pseudoRandomKey = hkdf.extract(protocolSalt, sharedSecret)
            val derivedSymmetricKey = hkdf.expand(pseudoRandomKey, sharedInfo, 32)

            return decrypt(sealedMessage.ciphertext, derivedSymmetricKey, iv)
        }


        fun generatedSharedSecret(
            otherUserPublicKey: PublicKey,
            selfPrivateKey: PrivateKey
        ): ByteArray? {
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(selfPrivateKey)
            keyAgreement.doPhase(otherUserPublicKey, true)
            return keyAgreement.generateSecret()
        }
    }
}