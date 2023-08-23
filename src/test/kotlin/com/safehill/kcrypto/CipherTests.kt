package com.safehill.kcrypto

import com.safehill.kcrypto.models.SHKeyPair
import com.safehill.kcrypto.models.SHSymmetricKey
import com.safehill.kcrypto.models.SHSymmetricKeySize
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class CipherTests {

    @Test
    fun testEncryptDecryptSharedSecretStaticIV() {
        val stringToEncrypt = "Text to encrypt"
        val encryptionKey = SHSymmetricKey().secretKeySpec.encoded

        // Encrypt
        val cipherText = SHCypher.encrypt(stringToEncrypt.toByteArray(), encryptionKey)
        assertNotNull(cipherText)

        // Base64 Encoded CipherText
        val cipherText2 = SHCypher.encrypt(stringToEncrypt.toByteArray(), encryptionKey)
        assertNotNull(cipherText2)
        assertEquals(Base64.getEncoder().encodeToString(cipherText), Base64.getEncoder().encodeToString(cipherText2))

        // Decrypt
        val decrypted = SHCypher.decrypt(cipherText, encryptionKey)
        assertEquals(stringToEncrypt, String(decrypted))
    }

    @Test
    fun testEncryptDecryptSharedSecret() {
        val base64Encoder = Base64.getEncoder()
        val stringToEncrypt = "Text to encrypt"
        val encryptionKey = SHSymmetricKey().secretKeySpec.encoded

        // Generate IV
        val iv = SHCypher.generateRandomIV()

        // Encrypt
        val cipherText = SHCypher.encrypt(stringToEncrypt.toByteArray(), encryptionKey, iv)
        assertNotNull(cipherText)

        // Base64 Encoded CipherText
        val cipherText2 = SHCypher.encrypt(stringToEncrypt.toByteArray(), encryptionKey, SHCypher.generateRandomIV())
        assertNotNull(cipherText2)
        val cypher1base1 = base64Encoder.encodeToString(cipherText)
        val cypher1base2: String = base64Encoder.encodeToString(cipherText)
        val cypher2base1: String = base64Encoder.encodeToString(cipherText2)
        val cypher2base2: String = base64Encoder.encodeToString(cipherText2)
        assertEquals(cypher1base1, cypher1base2)
        assertNotEquals(cypher1base1, cypher2base1)
        assertEquals(cypher2base1, cypher2base2)

        // Decrypt
        val decrypted = SHCypher.decrypt(cipherText, encryptionKey, iv)
        assertEquals(stringToEncrypt, String(decrypted))

        val base64Key = base64Encoder.encodeToString(encryptionKey)
        val base64IV = base64Encoder.encodeToString(iv)
        println("cypherBase64=$cypher1base1")
        println("keyBase64=$base64Key")
        println("ivBase64=$base64IV")
    }

    @Test
    fun testDecryptSwiftGeneratedSharedSecret() {
        val base64Encoder = Base64.getEncoder()
        val base64Decoder = Base64.getDecoder()

        val clearString = "This is our secret"
        val encrypted = "w4DK1kenpU3/FWDmFAHcR1L5Fv06B60o75TG+qXCIeFmuN5kzvWDfHJF8OKRlQ=="

        val encryptionKey = base64Decoder.decode("rC86lMjQvmUUKgnrTufzuwNyysbShyRPhjg2eORi6jQ=")
        val ivBase64 = encrypted.substring(0, 16)
        val cipherTextBase64 = encrypted.substring(16)
        val iv = base64Decoder.decode(ivBase64)
        val cipherText = base64Decoder.decode(cipherTextBase64)

        assertNotNull(iv)
        assertNotNull(cipherText)
        assertNotNull(encryptionKey)

        val kotlinEncrypted = SHCypher.encrypt(clearString.toByteArray(), encryptionKey, iv)
        assertEquals(cipherTextBase64, base64Encoder.encodeToString(kotlinEncrypted))

        val decrypted = SHCypher.decrypt(cipherText, encryptionKey, iv)
        val decryptedValue = String(decrypted)
        assertEquals(decryptedValue, clearString)
    }

    @Test
    fun testSimpleEncryptDecryptWithPublicKeySignature() {
        val string = "This is a test"
        val data = string.toByteArray()
        val senderSignatureKeys = SHKeyPair.generate()
        val receiverEncryptionKeys = SHKeyPair.generate()
        val ephemeralSecret = SHKeyPair.generate()

        val encrypted = SHCypher.encrypt(data, receiverEncryptionKeys.public, ephemeralSecret, senderSignatureKeys)
        val decrypted = SHCypher.decrypt(encrypted, receiverEncryptionKeys, senderSignatureKeys.public)

        assertEquals(String(data), String(decrypted))
    }

    @Test
    fun testEncryptDecryptWithPublicKeySignature() {
        val string = "This is a test"
        val data = string.toByteArray()

        val senderSignatureKeys = SHKeyPair.generate()
        val receiverEncryptionKeys = SHKeyPair.generate()

        val ephemeralSecret = SHKeyPair.generate()
        val sharedIV = SHCypher.generateRandomIV()

        val secret = SHSymmetricKey(SHSymmetricKeySize.BITS_256).secretKeySpec
        val encryptedDataWithSecret = SHCypher.encrypt(data, secret.encoded, sharedIV)
        val encryptedSecretUsingReceiverPublicKey = SHCypher.encrypt(secret.encoded, receiverEncryptionKeys.public, ephemeralSecret, senderSignatureKeys)

        /*
         SENDER shares `encryptedDataWithSecret` and `encryptedSecretUsingReceiverPublicKey` with RECEIVER.
         RECEIVER decrypts `encryptedSecretUsingReceiverPublicKey` to retrieve `decryptedSecret`, which can be used to decrypt `encryptedDataWithSecret`
         */
        val decryptedSecretBytes = SHCypher.decrypt(encryptedSecretUsingReceiverPublicKey, receiverEncryptionKeys, senderSignatureKeys.public)
        val decryptedSecret = SHSymmetricKey(decryptedSecretBytes).secretKeySpec
        val decryptedData = SHCypher.decrypt(encryptedDataWithSecret, decryptedSecret.encoded, sharedIV)
        val decryptedString = String(decryptedData)

        assertEquals(string, decryptedString)
    }
}