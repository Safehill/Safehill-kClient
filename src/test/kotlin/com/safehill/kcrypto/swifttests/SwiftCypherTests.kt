package com.safehill.kcrypto.swifttests

import com.safehill.kcrypto.SafehillCypher
import com.safehill.kcrypto.base64.base64EncodedString
import com.safehill.kcrypto.models.EncryptedData
import com.safehill.kcrypto.models.SafehillKeyPair
import com.safehill.kcrypto.models.LocalCryptoUser
import com.safehill.kcrypto.models.RemoteCryptoUser
import com.safehill.kcrypto.models.SymmetricKey
import com.safehill.kcrypto.models.SHUserContext
import com.safehill.kcrypto.models.SignatureVerificationError
import com.safehill.kcrypto.models.bytes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SwiftCypherTests {

    private val protocolSalt = SafehillCypher.generateRandomIV()

    @Test
    fun testEncryptDecryptSharedSecret() {
        val originalString = "This is our secret"
        val originalStringBytes = originalString.toByteArray()

        val key = SymmetricKey()
        val cipher = SafehillCypher.encrypt(originalStringBytes, key.secretKeySpec.encoded)

        /// Ensure 2 encryptions generate different results (randomness) and that base64 encoding is stable
        val cipher2 = SafehillCypher.encrypt(originalStringBytes, key.secretKeySpec.encoded)

        assertEquals(cipher.base64EncodedString(), cipher.base64EncodedString())
        assertNotEquals(cipher.base64EncodedString(), cipher2.base64EncodedString())
        assertEquals(cipher2.base64EncodedString(), cipher2.base64EncodedString())

        val decrypted = SafehillCypher.decrypt(cipher, key.secretKeySpec.encoded)
        val decryptedString = String(decrypted)

        assertEquals(originalString, decryptedString)
    }

    @Test
    fun testEncryptDecryptWithPublicKeySignature() {
        val string = "This is a test"
        val data = string.toByteArray()

        val senderSignatureKeys = SafehillKeyPair.generate()

        val receiverEncryptionKeys = SafehillKeyPair.generate()

        val ephemeralKey = SafehillKeyPair.generate()

        val secret = SymmetricKey()

        val encryptedDataWithSecret = SafehillCypher.encrypt(data, secret)

        val encryptedSecretWithReceiverPublicKey = SafehillCypher.encrypt(
            message = secret.secretKeySpec.encoded,
            receiverPublicKey = receiverEncryptionKeys.public,
            ephemeralKey = ephemeralKey,
            protocolSalt = protocolSalt,
            senderSignatureKey = senderSignatureKeys
        )

        val decryptedSecretData = SafehillCypher.decrypt(
            sealedMessage = encryptedSecretWithReceiverPublicKey,
            encryptionKey = receiverEncryptionKeys,
            protocolSalt = protocolSalt,
            signedBy = senderSignatureKeys.public
        )

        val decryptedSecret = SymmetricKey(decryptedSecretData)
        val decryptedData = SafehillCypher.decrypt(
            cipherText = encryptedDataWithSecret,
            key = decryptedSecret
        )

        val decryptedString = String(decryptedData)
        assertEquals(string, decryptedString)

    }

    @Test
    fun testShareablePayloadAliceAndBob() {
        val alice = LocalCryptoUser()
        val bob = LocalCryptoUser()
        val aliceContext = SHUserContext(alice)
        val bobContext = SHUserContext(bob)

        // Alice uploads encrypted content for Bob (and only Bob) to decrypt
        val originalString = "This is a test"
        val stringAsData = originalString.toByteArray(Charsets.UTF_8)

        val encryptedData = EncryptedData(stringAsData)

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
        val hacker = LocalCryptoUser()
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
        val alice = LocalCryptoUser()

        val originalString = "This is a test"
        val stringAsData = originalString.toByteArray(Charsets.UTF_8)
        val encryptedData = EncryptedData(stringAsData)

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
        val secret = SymmetricKey()
        assertEquals(SymmetricKey(secret.bytes), secret)

        val user1 = LocalCryptoUser()
        val user2 = LocalCryptoUser()

        // User 1 encrypts the secret for user 1 (self)
        val encryptedSecretForSelf = SHUserContext(user1).shareable(
            data = secret.bytes,
            protocolSalt = protocolSalt,
            with = RemoteCryptoUser(user1.publicKeyData, user1.publicSignatureData)
        )

        // User 1 decrypts the secret encoded with user1 public key
        val decryptedSecret = SafehillCypher.decrypt(
            sealedMessage = encryptedSecretForSelf,
            encryptionKey = user1.key,
            protocolSalt = protocolSalt,
            signedBy = user1.publicSignature
        )

        assert(secret.bytes.contentEquals(decryptedSecret))
        assertEquals(SymmetricKey(decryptedSecret), SymmetricKey(secret.bytes))

        // User 1 encrypts the secret for user 2
        val encryptedSecretForUser2 = SHUserContext(user1).shareable(
            data = secret.bytes,
            protocolSalt = protocolSalt,
            with = RemoteCryptoUser(user2.publicKeyData, user2.publicSignatureData)
        )
        // User 2 decrypts the secret encoded with user1 public key
        val decryptedSecret2 = SafehillCypher.decrypt(
            sealedMessage = encryptedSecretForUser2,
            encryptionKey = user2.key,
            protocolSalt = protocolSalt,
            signedBy = user1.publicSignature
        )
        assert(secret.bytes.contentEquals(decryptedSecret2))
        assertEquals(SymmetricKey(decryptedSecret2), SymmetricKey(secret.bytes))
    }
}