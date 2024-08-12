package com.safehill.kclient

import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @param digits number of digits in the otp.
 * @param validDuration the validity of otp.
 */
class SafehillOTP(
    digits: Int,
    validDuration: Duration
) {

    private val otpConfig = TimeBasedOneTimePasswordConfig(
        codeDigits = digits,
        hmacAlgorithm = HmacAlgorithm.SHA1,
        timeStep = validDuration.inWholeMilliseconds,
        timeStepUnit = TimeUnit.MILLISECONDS
    )

    /**
     * OTP code generation
     * @param secret the cryptographically-secure secret
     * @param instant the time at which the code should be generated
     * @return [OTPInfo] holding the code and validity duration.
     */
    fun generateCode(
        secret: ByteArray,
        instant: Instant
    ): OTPInfo {
        val generator = TimeBasedOneTimePasswordGenerator(secret, otpConfig)
        val otpCode = generator.generate(instant = instant)

        val counter = generator.counter()
        val endEpochMillis = generator.timeslotStart(counter + 1) - 1
        return OTPInfo(
            otp = otpCode,
            validity = (endEpochMillis - Instant.now().toEpochMilli()).milliseconds
        )
    }
}

data class OTPInfo(
    val otp: String,
    val validity: Duration
)