package crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// https://stackoverflow.com/questions/59577317/ios-cryptokit-in-java/59658891#59658891
// https://stackoverflow.com/questions/61332076/cross-platform-aes-encryption-between-ios-and-kotlin-java-using-apples-cryptokit

class SHCypher {

    companion object {

        val GCM_IV_LENGTH = 12
        val GCM_TAG_LENGTH = 16

        fun generateRandomIV(length: Int = GCM_IV_LENGTH): ByteArray {
            val sr = SecureRandom()
            val iv = ByteArray(length)
            sr.nextBytes(iv)
            return iv
        }

        fun encrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray? {
            // Get Cipher Instance
            val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")

            // Create SecretKeySpec
            val keySpec = SecretKeySpec(key, "AES")

            // Create GCMParameterSpec
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            // Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec)

            // Perform Encryption
//            return cipher.doFinal(plaintext)
            val cipherText = ByteArray(cipher.getOutputSize(plaintext.size))
            var ctLength = cipher.update(plaintext, 0, plaintext.size, cipherText, 0)
            ctLength += cipher.doFinal(cipherText, ctLength)
            return cipherText
        }

        fun decrypt(cipherText: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
            // Get Cipher Instance
            val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")

            // Create SecretKeySpec
            val keySpec = SecretKeySpec(key, "AES")

            // Create GCMParameterSpec
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            // Initialize Cipher for DECRYPT_MODE
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec)

            // Perform Decryption
            val decryptedText = cipher.doFinal(cipherText)
            return decryptedText
        }
    }
}