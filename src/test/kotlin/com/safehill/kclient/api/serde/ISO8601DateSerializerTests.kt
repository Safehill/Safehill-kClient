package com.safehill.kclient.api.serde

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals


class ISO8601DateSerializerTests {
    @Test
    fun testIso8601DateSerde() {
        assertEquals("1970-01-01T00:00:00.000Z", Date(0).toIso8601String())
        assert(Date(0).equals(Date(0).toIso8601String().toIso8601Date()))
        assert(Date().equals(Date().toIso8601String().toIso8601Date()))
    }
}