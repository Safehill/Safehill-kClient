package com.safehill.kclient

import com.safehill.kclient.models.SafehillKeyPair
import org.junit.jupiter.api.Test

class SafehillCypherTest {

    @Test
    fun `shared secret can be generated between 2 parties`() {
        val bobKeys = SafehillKeyPair.generate()
        val aliceKeys = SafehillKeyPair.generate()

        val sharedKeyForAlice = SafehillCypher.generatedSharedSecret(
            otherUserPublicKey = bobKeys.public,
            selfPrivateKey = aliceKeys.private
        )

        val sharedKeyForBob = SafehillCypher.generatedSharedSecret(
            otherUserPublicKey = aliceKeys.public,
            selfPrivateKey = bobKeys.private
        )
        assert(
            sharedKeyForAlice.contentEquals(sharedKeyForBob)
        )
    }
}