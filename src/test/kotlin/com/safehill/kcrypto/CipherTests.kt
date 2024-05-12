package com.safehill.kcrypto

import com.safehill.kclient.api.SafehillApiImpl
import com.safehill.kclient.api.dtos.AuthChallengeResponseDTO
import com.safehill.kclient.api.dtos.AuthResolvedChallengeDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kcrypto.models.SafehillKeyPair
import com.safehill.kcrypto.models.LocalCryptoUser
import com.safehill.kcrypto.models.SafehillPrivateKey
import com.safehill.kcrypto.models.SafehillPublicKey
import com.safehill.kcrypto.models.ShareablePayload
import com.safehill.kcrypto.models.SafehillSignature
import com.safehill.kcrypto.models.SymmetricKey
import com.safehill.kcrypto.models.SymmetricKeySize
import com.safehill.kcrypto.models.SHUserContext
import org.junit.jupiter.api.Test
import java.util.Arrays
import java.util.Base64
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class CipherTests {

    private val STATIC_PROTOCOL_SALT: ByteArray = Base64.getDecoder().decode("/5RWVwIP//+i///Z")

    @Test
    fun testGenerateOTP() {
        val secret = SafehillCypher.generateRandomIV()
        val (code1, _) = SafehillCypher.generateOTPCode(secret = secret, digits = 6)
        assertEquals(code1.length, 6)

        val (code2, _) = SafehillCypher.generateOTPCode(secret = secret, digits = 6)
        assertEquals(code1.length, 6)
        assertEquals(code1, code2)

        val (code3, _) = SafehillCypher.generateOTPCode(
            secret = secret,
            digits = 6,
            timeStepInSeconds = 1
        )
        val (code4, valid4) = SafehillCypher.generateOTPCode(
            secret = secret,
            digits = 6,
            timeStepInSeconds = 1
        )
        assertEquals(code3, code4)

        Thread.sleep(valid4 + 1)

        val (code5, _) = SafehillCypher.generateOTPCode(
            secret = secret,
            digits = 6,
            timeStepInSeconds = 1
        )
        assertNotEquals(code4, code5)

        val (code6, _) = SafehillCypher.generateOTPCode(
            secret = secret,
            digits = 6,
            timeStepInSeconds = 2
        )
        assertNotEquals(code5, code6)

        val (code7, _) = SafehillCypher.generateOTPCode(
            secret = secret,
            digits = 6,
            timeStepInSeconds = 2
        )
        assertEquals(code6, code7)

        Thread.sleep(1100)

        val (code8, valid8) = SafehillCypher.generateOTPCode(
            secret = secret,
            digits = 6,
            timeStepInSeconds = 2
        )
        Thread.sleep(max(0, valid8 - 100))
        val (code9, valid9) = SafehillCypher.generateOTPCode(
            secret = secret,
            digits = 6,
            timeStepInSeconds = 2
        )
        assertEquals(code8, code9)
        Thread.sleep(valid9 + 1)
        val (code10, _) = SafehillCypher.generateOTPCode(
            secret = secret,
            digits = 6,
            timeStepInSeconds = 2
        )
        assertNotEquals(code9, code10)

        val newSecret = SafehillCypher.generateRandomIV()
        val (code11, valid11) = SafehillCypher.generateOTPCode(
            secret = newSecret,
            digits = 6,
            timeStepInSeconds = 2
        )
        Thread.sleep(max(0, valid11 - 100))
        val (code12, _) = SafehillCypher.generateOTPCode(
            secret = newSecret,
            digits = 6,
            timeStepInSeconds = 2
        )
        assertEquals(code11, code12)
    }

    @Test
    fun testEncryptDecryptSharedSecretStaticIV() {
        val stringToEncrypt = "Text to encrypt"
        val encryptionKey = SymmetricKey().secretKeySpec.encoded

        val STATIC_IV: ByteArray = Base64.getDecoder().decode("/5RWVwIP//+i///Z")

        // Encrypt
        val cipherText =
            SafehillCypher.encrypt(stringToEncrypt.toByteArray(), encryptionKey, iv = STATIC_IV)
        assertNotNull(cipherText)

        // Base64 Encoded CipherText
        val cipherText2 =
            SafehillCypher.encrypt(stringToEncrypt.toByteArray(), encryptionKey, iv = STATIC_IV)
        assertNotNull(cipherText2)
        assertEquals(
            Base64.getEncoder().encodeToString(cipherText),
            Base64.getEncoder().encodeToString(cipherText2)
        )

        // Decrypt
        val decrypted = SafehillCypher.decrypt(cipherText, encryptionKey)
        assertEquals(stringToEncrypt, String(decrypted))
    }

    @Test
    fun testEncryptDecryptSharedSecret() {
        val base64Encoder = Base64.getEncoder()
        val stringToEncrypt = "Text to encrypt"
        val encryptionKey = SymmetricKey().secretKeySpec.encoded

        // Generate IV
        val iv = SafehillCypher.generateRandomIV()

        // Encrypt
        val cipherText = SafehillCypher.encrypt(stringToEncrypt.toByteArray(), encryptionKey, iv)
        assertNotNull(cipherText)

        // Base64 Encoded CipherText
        val cipherText2 = SafehillCypher.encrypt(
            stringToEncrypt.toByteArray(),
            encryptionKey,
            SafehillCypher.generateRandomIV()
        )
        assertNotNull(cipherText2)
        val cypher1base1 = base64Encoder.encodeToString(cipherText)
        val cypher1base2: String = base64Encoder.encodeToString(cipherText)
        val cypher2base1: String = base64Encoder.encodeToString(cipherText2)
        val cypher2base2: String = base64Encoder.encodeToString(cipherText2)
        assertEquals(cypher1base1, cypher1base2)
        assertNotEquals(cypher1base1, cypher2base1)
        assertEquals(cypher2base1, cypher2base2)

        // Decrypt
        val decrypted = SafehillCypher.decrypt(cipherText, encryptionKey)
        assertEquals(stringToEncrypt, String(decrypted))

        val base64Key = base64Encoder.encodeToString(encryptionKey)
        val base64IV = base64Encoder.encodeToString(iv)
        println("cypherBase64=$cypher1base1")
        println("keyBase64=$base64Key")
        println("ivBase64=$base64IV")
    }

    //    @Test
    fun testDecryptSwiftGeneratedSharedSecret() {
        val base64Encoder = Base64.getEncoder()
        val base64Decoder = Base64.getDecoder()

        val clearString = "This is our secret"
        val encryptionKey = base64Decoder.decode("rC86lMjQvmUUKgnrTufzuwNyysbShyRPhjg2eORi6jQ=")

        val encrypted = "w4DK1kenpU3/FWDmFAHcR1L5Fv06B60o75TG+qXCIeFmuN5kzvWDfHJF8OKRlQ=="

        val ivBase64 = encrypted.substring(0, 16)
        val cipherTextBase64 = encrypted.substring(16)
        val iv = base64Decoder.decode(ivBase64)
        val cipherText = base64Decoder.decode(cipherTextBase64)

        assertNotNull(iv)
        assertNotNull(cipherText)
        assertNotNull(encryptionKey)

        val kotlinEncrypted = SafehillCypher.encrypt(clearString.toByteArray(), encryptionKey, iv)
        assertEquals(cipherTextBase64, base64Encoder.encodeToString(kotlinEncrypted))

        val decrypted = SafehillCypher.decrypt(cipherText, encryptionKey, iv)
        val decryptedValue = String(decrypted)
        assertEquals(decryptedValue, clearString)

        // Assert the `SHCypher.decrypt` method is flexible enough to deal with Swift-encrypted values
        // By applying the split implemented above on `AEADBadTagException` on first try
        val cipherText2 = base64Decoder.decode(encrypted)
        val decrypted2 = SafehillCypher.decrypt(cipherText2, encryptionKey)
        val decryptedValue2 = String(decrypted2)
        assertEquals(decryptedValue2, clearString)
    }

    @Test
    fun testSimpleEncryptDecryptWithPublicKeySignature() {
        val string = "This is a test"
        val data = string.toByteArray()
        val senderSignatureKeys = SafehillKeyPair.generate()
        val receiverEncryptionKeys = SafehillKeyPair.generate()
        val ephemeralSecret = SafehillKeyPair.generate()

        val encrypted = SafehillCypher.encrypt(
            message = data,
            receiverPublicKey = receiverEncryptionKeys.public,
            ephemeralKey = ephemeralSecret,
            protocolSalt = STATIC_PROTOCOL_SALT,
            senderSignatureKey = senderSignatureKeys
        )
        val decrypted = SafehillCypher.decrypt(
            sealedMessage = encrypted,
            encryptionKey = receiverEncryptionKeys,
            protocolSalt = STATIC_PROTOCOL_SALT,
            signedBy = senderSignatureKeys.public
        )

        assertEquals(String(data), String(decrypted))
    }

    @Test
    fun testEncryptDecryptWithPublicKeySignature() {
        val string = "This is a test"
        val data = string.toByteArray()

        val senderSignatureKeys = SafehillKeyPair.generate()
        val receiverEncryptionKeys = SafehillKeyPair.generate()

        val ephemeralSecret = SafehillKeyPair.generate()
        val sharedIV = SafehillCypher.generateRandomIV()

        val secret = SymmetricKey(SymmetricKeySize.BITS_256).secretKeySpec
        val encryptedDataWithSecret = SafehillCypher.encrypt(data, secret.encoded, sharedIV)
        val encryptedSecretUsingReceiverPublicKey = SafehillCypher.encrypt(
            message = secret.encoded,
            receiverPublicKey = receiverEncryptionKeys.public,
            ephemeralKey = ephemeralSecret,
            protocolSalt = STATIC_PROTOCOL_SALT,
            senderSignatureKey = senderSignatureKeys
        )

        /*
         SENDER shares `encryptedDataWithSecret` and `encryptedSecretUsingReceiverPublicKey` with RECEIVER.
         RECEIVER decrypts `encryptedSecretUsingReceiverPublicKey` to retrieve `decryptedSecret`, which can be used to decrypt `encryptedDataWithSecret`
         */
        val decryptedSecretBytes = SafehillCypher.decrypt(
            sealedMessage = encryptedSecretUsingReceiverPublicKey,
            encryptionKey = receiverEncryptionKeys,
            protocolSalt = STATIC_PROTOCOL_SALT,
            signedBy = senderSignatureKeys.public
        )
        val decryptedSecret = SymmetricKey(decryptedSecretBytes).secretKeySpec
        val decryptedData = SafehillCypher.decrypt(encryptedDataWithSecret, decryptedSecret.encoded)
        val decryptedString = String(decryptedData)

        assertEquals(string, decryptedString)
    }

    //    @Test
    fun testEncryptDecryptWithPublicKeySignatureSwiftEquivalent() {
        val string = "This is a test"

        val protocolSalt = Base64.getDecoder().decode("/5RWVwIP//+i///Z")
        val iv = Base64.getDecoder().decode("/5RWVwIP//+i///Z")

        val senderSignatureBase64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEk5nINqQDigFdTIEI5BJ1o4E72RDs4S7qi1/9dYRGcLQhENITPpM9jYM7KMpeg1/xgTFWZL+pk9rhfNorHOat5A=="
        val senderSignature = SafehillPublicKey.from(Base64.getDecoder().decode(senderSignatureBase64))

        val receiverPrivateKeyBase64 =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgZco8S8aWD0NBYHcRvuu/xhdY7b1YcnxkkjwOuXdKlOqhRANCAATbl2f801RNl2FIY2F/p2G0nydd2Wy6Kzo7i1Er8fGUnE97Nh+RvUYz+J7MxS4mek29n4OF4Aj14veEmojDTucI"
        val receiverPrivateKey =
            SafehillPrivateKey.from(Base64.getDecoder().decode(receiverPrivateKeyBase64))
        val receiverPublicKeyBase64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE25dn/NNUTZdhSGNhf6dhtJ8nXdlsuis6O4tRK/HxlJxPezYfkb1GM/iezMUuJnpNvZ+DheAI9eL3hJqIw07nCA=="
        val receiverPublicKey =
            SafehillPublicKey.from(Base64.getDecoder().decode(receiverPublicKeyBase64))

        val sharedIV = SafehillCypher.generateRandomIV()

        val encryptedDataWithSecret =
            Base64.getDecoder().decode("IsYvvvsGrKG0/Y4LmtvAFX7DHCJsVbH5b5wzsV/u6BOCNGT10x9tnFqX")
        val ephemeralPublicKeyDataBase64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEQiSI165lDlgUxD1gRt0GFT9QYHzMcKNskxaEtJAPqrQdD7ZaTWWDAJexaIw6Tg4dzqtn7gTcLOYa66eBmfhhPw=="
        val cipherTextBase64 =
            "ZSwl2IVHKc5fbdjxt7Q1Qy2kFGk9E65fs6OMHo+3SpVUI3L5m2cuzqGvWaQ57yM8JmyFHPxIzIm5DYFG"
        val signatureBase64 =
            "MEYCIQDSLaclSCN3gC07e5GOiJH1KNz6+YkybcYlaJlVf0YLHwIhAPEEXOPp6I1I03/YyCFx8II/yXyapwa1BqXbiEDdepGR"

        val encryptedSecretUsingReceiverPublicKey = ShareablePayload(
            Base64.getDecoder().decode(ephemeralPublicKeyDataBase64),
            Base64.getDecoder().decode(cipherTextBase64),
            Base64.getDecoder().decode(signatureBase64),
            null
        )

        /*
         SENDER shares `encryptedDataWithSecret` and `encryptedSecretUsingReceiverPublicKey` with RECEIVER.
         RECEIVER decrypts `encryptedSecretUsingReceiverPublicKey` to retrieve `decryptedSecret`, which can be used to decrypt `encryptedDataWithSecret`
         */
        val decryptedSecretBytes = SafehillCypher.decrypt(
            sealedMessage = encryptedSecretUsingReceiverPublicKey,
            userPrivateKey = receiverPrivateKey,
            userPublicKey = receiverPublicKey,
            protocolSalt = protocolSalt,
            iv = iv,
            signedBy = senderSignature
        )
        val decryptedSecret = SymmetricKey(decryptedSecretBytes).secretKeySpec
        val decryptedData =
            SafehillCypher.decrypt(encryptedDataWithSecret, decryptedSecret.encoded, sharedIV)
        val decryptedString = String(decryptedData)

        assertEquals(string, decryptedString)
    }

    @Test
    fun testServerAuthenticationChallenge() {
        val clientUser = LocalCryptoUser()
        val serverUser = LocalCryptoUser()

        // SERVER creates challenge (prefixed with /**/ are server executions in a real world scenario)
        /**/
        val challenge = SymmetricKey(SymmetricKeySize.BITS_128).secretKeySpec.encoded
        /**/
        val encryptedChallenge = SHUserContext(serverUser).shareable(
            challenge,
            clientUser,
            STATIC_PROTOCOL_SALT,
        )

        // Check the client would be able to verify it
        assert(
            SafehillSignature.verify(
                encryptedChallenge.ciphertext + encryptedChallenge.ephemeralPublicKeyData + clientUser.key.public.encoded,
                encryptedChallenge.signature,
                serverUser.publicSignature
            )
        )

        // Then SERVER sends an SHAuthChallenge to the client
        val challengeBase64 = Base64.getEncoder().encodeToString(encryptedChallenge.ciphertext)
        val ephemeralPublicKeyBase64 =
            Base64.getEncoder().encodeToString(encryptedChallenge.ephemeralPublicKeyData)
        val ephemeralPublicSignatureBase64 =
            Base64.getEncoder().encodeToString(encryptedChallenge.signature)
        val publicKeyBase64 = Base64.getEncoder().encodeToString(serverUser.publicKeyData)
        val publicSignatureBase64 =
            Base64.getEncoder().encodeToString(serverUser.publicSignatureData)
        val protocolSalt = Base64.getEncoder().encodeToString(STATIC_PROTOCOL_SALT)

        // Ensure nothing gets lost during SerDe
        assert(
            Arrays.equals(
                Base64.getDecoder().decode(challengeBase64),
                encryptedChallenge.ciphertext
            )
        )
        assert(
            Arrays.equals(
                Base64.getDecoder().decode(ephemeralPublicKeyBase64),
                encryptedChallenge.ephemeralPublicKeyData
            )
        )
        assert(
            Arrays.equals(
                Base64.getDecoder().decode(ephemeralPublicSignatureBase64),
                encryptedChallenge.signature
            )
        )
        assert(Arrays.equals(Base64.getDecoder().decode(publicKeyBase64), serverUser.publicKeyData))
        assert(
            Arrays.equals(
                Base64.getDecoder().decode(publicSignatureBase64),
                serverUser.publicSignatureData
            )
        )

        val authChallenge = AuthChallengeResponseDTO(
            challengeBase64,
            ephemeralPublicKeyBase64,
            ephemeralPublicSignatureBase64,
            publicKeyBase64,
            publicSignatureBase64,
            protocolSalt,
            null
        )

        // Client solves the challenge
        val solvedChallenge: AuthResolvedChallengeDTO =
            SafehillApiImpl(LocalUser(clientUser)).solveChallenge(authChallenge)

        val signedChallenge = Base64.getDecoder().decode(solvedChallenge.signedChallenge)
        val digest = Base64.getDecoder().decode(solvedChallenge.digest)
        val signedDigest = Base64.getDecoder().decode(solvedChallenge.signedDigest)

        // SERVER verifies the solved challenge
        /**/ assert(SafehillSignature.verify(challenge, signedChallenge, clientUser.publicSignature))
        /**/ assert(SafehillSignature.verify(digest, signedDigest, clientUser.publicSignature))
    }
}