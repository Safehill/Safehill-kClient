package com.safehill.kcrypto.swifttests

import com.safehill.kcrypto.SHCypher
import com.safehill.kcrypto.base64.base64EncodedString
import com.safehill.kcrypto.models.SHEncryptedData
import com.safehill.kcrypto.models.SHKeyPair
import com.safehill.kcrypto.models.SHLocalCryptoUser
import com.safehill.kcrypto.models.SHRemoteCryptoUser
import com.safehill.kcrypto.models.SHSymmetricKey
import com.safehill.kcrypto.models.SHUserContext
import com.safehill.kcrypto.models.SignatureVerificationError
import com.safehill.kcrypto.models.bytes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SwiftCypherTests {

    private val protocolSalt = SHCypher.generateRandomIV()

    @Test
    fun testEncryptDecryptSharedSecret() {
        val originalString = "This is our secret"
        val originalStringBytes = originalString.toByteArray()

        val key = SHSymmetricKey()
        val cipher = SHCypher.encrypt(originalStringBytes, key.secretKeySpec.encoded)

        /// Ensure 2 encryptions generate different results (randomness) and that base64 encoding is stable
        val cipher2 = SHCypher.encrypt(originalStringBytes, key.secretKeySpec.encoded)

        assertEquals(cipher.base64EncodedString(), cipher.base64EncodedString())
        assertNotEquals(cipher.base64EncodedString(), cipher2.base64EncodedString())
        assertEquals(cipher2.base64EncodedString(), cipher2.base64EncodedString())

        val decrypted = SHCypher.decrypt(cipher, key.secretKeySpec.encoded)
        val decryptedString = String(decrypted)

        assertEquals(originalString, decryptedString)
    }

    @Test
    fun testEncryptDecryptWithPublicKeySignature() {
        val string = "This is a test"
        val data = string.toByteArray()

        val senderSignatureKeys = SHKeyPair.generate()

        val receiverEncryptionKeys = SHKeyPair.generate()

        val ephemeralKey = SHKeyPair.generate()

        val secret = SHSymmetricKey()

        val encryptedDataWithSecret = SHCypher.encrypt(data, secret)

        val encryptedSecretWithReceiverPublicKey = SHCypher.encrypt(
            message = secret.secretKeySpec.encoded,
            receiverPublicKey = receiverEncryptionKeys.public,
            ephemeralKey = ephemeralKey,
            protocolSalt = protocolSalt,
            senderSignatureKey = senderSignatureKeys
        )

        val decryptedSecretData = SHCypher.decrypt(
            sealedMessage = encryptedSecretWithReceiverPublicKey,
            encryptionKey = receiverEncryptionKeys,
            protocolSalt = protocolSalt,
            signedBy = senderSignatureKeys.public
        )

        val decryptedSecret = SHSymmetricKey(decryptedSecretData)
        val decryptedData = SHCypher.decrypt(
            cipherText = encryptedDataWithSecret,
            key = decryptedSecret
        )

        val decryptedString = String(decryptedData)
        assertEquals(string, decryptedString)

    }

    @Test
    fun testShareablePayloadAliceAndBob() {
        val alice = SHLocalCryptoUser()
        val bob = SHLocalCryptoUser()
        val aliceContext = SHUserContext(alice)
        val bobContext = SHUserContext(bob)

        // Alice uploads encrypted content for Bob (and only Bob) to decrypt
        val originalString = "This is a test"
        val stringAsData = originalString.toByteArray(Charsets.UTF_8)

        val encryptedData = SHEncryptedData(stringAsData)

        // Upload encrypted data
        val encryptedSecret = aliceContext.shareable(
            data = encryptedData.privateSecret.bytes,
            with = bob,
            protocolSalt = protocolSalt
        )

        // Upload encrypted secret

        // Once Bob gets encryptedData, encryptedSecret
        val decryptedData = bobContext.decrypt(
            encryptedData.encryptedData,
            encryptedSecret,
            protocolSalt,
            alice
        )
        val decryptedString = decryptedData.decodeToString()

        assertEquals(originalString, decryptedString)

        // Ensure another user in possession of Alice's signature and public key can NOT decrypt that content
        val hacker = SHLocalCryptoUser()
        val hackerContext = SHUserContext(hacker)

        assertFailsWith(SignatureVerificationError::class) {
            hackerContext.decrypt(
                encryptedData.encryptedData,
                encryptedSecret,
                protocolSalt,
                alice
            )
        }

        // Ensure that if Alice's private key is compromised, the message for Bob still can't get decrypted
        assertFailsWith(SignatureVerificationError::class) {
            aliceContext.decrypt(
                encryptedData.encryptedData,
                encryptedSecret,
                protocolSalt,
                alice
            )
        }
    }

    @Test
    fun testShareablePayloadAliceToSelf() {
        val alice = SHLocalCryptoUser()

        val originalString = "This is a test"
        val stringAsData = originalString.toByteArray(Charsets.UTF_8)
        val encryptedData = SHEncryptedData(stringAsData)

        // Upload encrypted data
        val aliceContext = SHUserContext(alice)
        val encryptedSecret = aliceContext.shareable(
            data = encryptedData.privateSecret.bytes,
            protocolSalt = protocolSalt,
            with = alice
        )

        // Upload encrypted secret

        // Once Alice gets encryptedData, encryptedSecret
        val decryptedData = aliceContext.decrypt(
            encryptedData.encryptedData,
            encryptedSecret,
            protocolSalt,
            alice
        )
        val decryptedString = decryptedData.decodeToString()

        assertEquals(originalString, decryptedString)
    }

    @Test
    fun testDerivedSymmetricKey() {
        val secret = SHSymmetricKey()
        assertEquals(SHSymmetricKey(secret.bytes), secret)

        val user1 = SHLocalCryptoUser()
        val user2 = SHLocalCryptoUser()

        // User 1 encrypts the secret for user 1 (self)
        val encryptedSecretForSelf = SHUserContext(user1).shareable(
            data = secret.bytes,
            protocolSalt = protocolSalt,
            with = SHRemoteCryptoUser(user1.publicKeyData, user1.publicSignatureData)
        )

        // User 1 decrypts the secret encoded with user1 public key
        val decryptedSecret = SHCypher.decrypt(
            sealedMessage = encryptedSecretForSelf,
            encryptionKey = user1.key,
            protocolSalt = protocolSalt,
            signedBy = user1.publicSignature
        )

        assert(secret.bytes.contentEquals(decryptedSecret))
        assertEquals(SHSymmetricKey(decryptedSecret), SHSymmetricKey(secret.bytes))

        // User 1 encrypts the secret for user 2
        val encryptedSecretForUser2 = SHUserContext(user1).shareable(
            data = secret.bytes,
            protocolSalt = protocolSalt,
            with = SHRemoteCryptoUser(user2.publicKeyData, user2.publicSignatureData)
        )
        // User 2 decrypts the secret encoded with user1 public key
        val decryptedSecret2 = SHCypher.decrypt(
            sealedMessage = encryptedSecretForUser2,
            encryptionKey = user2.key,
            protocolSalt = protocolSalt,
            signedBy = user1.publicSignature
        )
        assert(secret.bytes.contentEquals(decryptedSecret2))
        assertEquals(SHSymmetricKey(decryptedSecret2), SHSymmetricKey(secret.bytes))
    }
}