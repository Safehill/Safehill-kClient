package com.safehill.kcrypto

import com.safehill.kcrypto.models.SHKeyPair
import org.junit.jupiter.api.Test

class SHCypherTest {

    @Test
    fun `test shared secret can be generated between 2 parties`() {
        val bobKeys = SHKeyPair.generate()
        val aliceKeys = SHKeyPair.generate()

        val sharedKeyForAlice = SHCypher.generateSharedKey(
            otherUserPublicKey = bobKeys.public,
            selfPrivateKey = aliceKeys.private
        )

        val sharedKeyForBob = SHCypher.generateSharedKey(
            otherUserPublicKey = aliceKeys.public,
            selfPrivateKey = bobKeys.private
        )
        assert(
            sharedKeyForAlice.contentEquals(sharedKeyForBob)
        )
    }
}