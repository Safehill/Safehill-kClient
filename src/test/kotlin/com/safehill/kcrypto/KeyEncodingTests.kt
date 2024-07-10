package com.safehill.kcrypto

import com.safehill.kcrypto.models.SafehillKeyPair
import com.safehill.kcrypto.models.SafehillPrivateKey
import com.safehill.kcrypto.models.SafehillPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.Test
import java.security.Security
import java.util.*
import kotlin.test.assertEquals

class KeyEncodingTests {
    @Test
    fun testPrivateKeyEncodeDecode() {
        val base64Encoder = Base64.getEncoder()

        val keyPair = SafehillKeyPair.generate()
        val privateKey = SafehillPrivateKey.from(keyPair.private.encoded)

        assertEquals(
            base64Encoder.encodeToString(keyPair.private.encoded),
            base64Encoder.encodeToString(privateKey.encoded)
        )
    }

    @Test
    fun testPublicKeyEncodeDecode() {
        val base64Encoder = Base64.getEncoder()

        val keyPair = SafehillKeyPair.generate()
        val publicKey = SafehillPublicKey.from(keyPair.public.encoded)

        assertEquals(
            base64Encoder.encodeToString(keyPair.public.encoded),
            base64Encoder.encodeToString(publicKey.encoded)
        )
    }

    @Test
    fun testPemGeneratedKeys() {
        val base64Encoder = Base64.getEncoder()
        val base64Decoder = Base64.getDecoder()

        val base64SwiftPrivateSignature = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgPGbllKQ6ya84JiFd7zB+zD93ucJnAAgqOIrT2gNF5G+hRANCAAR4wKOOJp0UR6IWDDhX9tRPxgJUDL61dIcZrHZsK3X/l0WHGWwzRrdOYt1M40uKQ6uR3iDbk+SpuoNPXvcKpRuM"
        val privateKey = SafehillPrivateKey.from(base64Decoder.decode(base64SwiftPrivateSignature))
        assertEquals(base64Encoder.encodeToString(privateKey.encoded), base64SwiftPrivateSignature)

        val base64DerPublicSignatureKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEhiApWzykjNlZ4EZ5QIzIeoLG5Gf9vkwxaY0lqg7Z4C1otPvWdK584Sm61bAwF2MByrAsa5PgdZkn0eJ6X0qw0g=="
        val publicSignatureKey = SafehillPublicKey.from(base64Decoder.decode(base64DerPublicSignatureKey))
        assertEquals(base64Encoder.encodeToString(publicSignatureKey.encoded), base64DerPublicSignatureKey)

        val base64SwiftPrivateEncryptionKey = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgQfcAyw4ke+CJwQzHY6SLP1mI9RoYstMTzauJtWZQbUqhRANCAAR1C/90XdbTrolDqyZ1/7oKdxaxA0jPVxaeF6h0qrrR8SLbs6yfdbc5FcwvpFJ6mK+L1ubUnUZCgIkatnPDt56E"
        val privateEncryptionKey = SafehillPrivateKey.from(base64Decoder.decode(base64SwiftPrivateEncryptionKey))
        assertEquals(base64Encoder.encodeToString(privateEncryptionKey.encoded), base64SwiftPrivateEncryptionKey)

        val base64SwiftPublicEncryptionKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEdQv/dF3W066JQ6smdf+6CncWsQNIz1cWnheodKq60fEi27Osn3W3ORXML6RSepivi9bm1J1GQoCJGrZzw7eehA=="
        val publicEncryptionKey = SafehillPublicKey.from(base64Decoder.decode(base64SwiftPublicEncryptionKey))
        assertEquals(base64Encoder.encodeToString(publicEncryptionKey.encoded), base64SwiftPublicEncryptionKey)
    }

    @Test
    fun testDerivedPublicKey() {
        // Add Bouncy Castle as a security provider
        Security.addProvider(BouncyCastleProvider())

        val base64Encoder = Base64.getEncoder()
        val base64Decoder = Base64.getDecoder()

        val keyPair = SafehillKeyPair.generate()
        val publicKey1 = keyPair.public
        val publicKey2 = SafehillPublicKey.from(keyPair.public.encoded)

        val derivedPublicKey = SafehillPublicKey.derivePublicKeyFrom(keyPair.private)
        assertEquals(publicKey1, derivedPublicKey)
        assertEquals(publicKey2, derivedPublicKey)

        assertEquals(base64Encoder.encodeToString(publicKey1.encoded), base64Encoder.encodeToString(derivedPublicKey.encoded))

        val base64SwiftPrivateKey = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgGVu9VyaNfT3GraoD12earc+RM1pZo/KSKn2n9X/YPpyhRANCAAQJhMkcCFc6Qyc3aTR5cbHY649qQ5+xFeR/aA5u72SHXHiyElWx6z6dBeeQfLd1EfUyxdlJGkr1mCqBm0GPD068"
        val privateEncryptionKey = SafehillPrivateKey.from(base64Decoder.decode(base64SwiftPrivateKey))
        val expectedBase64SwiftPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAECYTJHAhXOkMnN2k0eXGx2OuPakOfsRXkf2gObu9kh1x4shJVses+nQXnkHy3dRH1MsXZSRpK9ZgqgZtBjw9OvA=="
        val expectedPublicKey = SafehillPublicKey.from(base64Decoder.decode(expectedBase64SwiftPublicKey))

        val derivedPublicKey2 = SafehillPublicKey.derivePublicKeyFrom(privateEncryptionKey)
        assertEquals(base64Encoder.encodeToString(expectedPublicKey.encoded), base64Encoder.encodeToString(derivedPublicKey2.encoded))
        assertEquals(expectedBase64SwiftPublicKey, base64Encoder.encodeToString(derivedPublicKey2.encoded))
    }
}