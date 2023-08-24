package com.safehill.kcrypto

import com.safehill.kcrypto.models.SHHash
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class SHCryptoUserTests {

    @Test
    fun testPrivateKeyEncodeDecode() {
        val dataBase64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEFp2uNurkYUh3U7O9m/wO+Oqcwnisxs97I7EmYuuGh3z4t72rNyI/WZcB+5DITlS4L0ydZhF8FAzv5FLMPmE5lw=="
        val data = Base64.getDecoder().decode(dataBase64)

        assertEquals(SHHash.stringDigest_legacyVersion(data), SHHash.stringDigest(data))
    }
}