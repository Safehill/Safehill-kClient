package crypto

import crypto.models.SHKeyPair
import crypto.models.SHPrivateKey
import crypto.models.SHPublicKey
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class KeyEncodingTests {
    @Test
    fun testPrivateKeyEncodeDecode() {
        val base64Encoder = Base64.getEncoder()

        val keyPair = SHKeyPair.generate()
        val privateKey = SHPrivateKey.from(keyPair.private.encoded)

        assertEquals(
            base64Encoder.encodeToString(keyPair.private.encoded),
            base64Encoder.encodeToString(privateKey.encoded)
        )
    }

    @Test
    fun testPublicKeyEncodeDecode() {
        val base64Encoder = Base64.getEncoder()

        val keyPair = SHKeyPair.generate()
        val publicKey = SHPublicKey.from(keyPair.public.encoded)

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
        val privateKey = SHPrivateKey.from(base64Decoder.decode(base64SwiftPrivateSignature))
        assertEquals(base64Encoder.encodeToString(privateKey.encoded), base64SwiftPrivateSignature)

        val base64DerPublicSignatureKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEhiApWzykjNlZ4EZ5QIzIeoLG5Gf9vkwxaY0lqg7Z4C1otPvWdK584Sm61bAwF2MByrAsa5PgdZkn0eJ6X0qw0g=="
        val publicSignatureKey = SHPublicKey.from(base64Decoder.decode(base64DerPublicSignatureKey))
        assertEquals(base64Encoder.encodeToString(publicSignatureKey.encoded), base64DerPublicSignatureKey)

        val base64SwiftPrivateEncryptionKey = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgQfcAyw4ke+CJwQzHY6SLP1mI9RoYstMTzauJtWZQbUqhRANCAAR1C/90XdbTrolDqyZ1/7oKdxaxA0jPVxaeF6h0qrrR8SLbs6yfdbc5FcwvpFJ6mK+L1ubUnUZCgIkatnPDt56E"
        val privateEncryptionKey = SHPrivateKey.from(base64Decoder.decode(base64SwiftPrivateEncryptionKey))
        assertEquals(base64Encoder.encodeToString(privateEncryptionKey.encoded), base64SwiftPrivateEncryptionKey)

        val base64SwiftPublicEncryptionKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEdQv/dF3W066JQ6smdf+6CncWsQNIz1cWnheodKq60fEi27Osn3W3ORXML6RSepivi9bm1J1GQoCJGrZzw7eehA=="
        val publicEncryptionKey = SHPublicKey.from(base64Decoder.decode(base64SwiftPublicEncryptionKey))
        assertEquals(base64Encoder.encodeToString(publicEncryptionKey.encoded), base64SwiftPublicEncryptionKey)

    }
}