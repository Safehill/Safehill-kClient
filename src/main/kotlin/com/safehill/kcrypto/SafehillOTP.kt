package com.safehill.kcrypto

import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

/**
 * @param digits number of digits in the otp.
 * @param validDuration the validity of otp.
 * The generated otp will be the same for half the time step if the same secret is used.
 * Use [isValid] to check if the otp is valid within the validity duration.
 */
class SafehillOTP(
    digits: Int,
    validDuration: Duration
) {
    private val halfTimeStep = validDuration / 2

    private val otpConfig = TimeBasedOneTimePasswordConfig(
        codeDigits = digits,
        hmacAlgorithm = HmacAlgorithm.SHA1,
        timeStep = halfTimeStep.inWholeMicroseconds,
        timeStepUnit = TimeUnit.MICROSECONDS
    )

    /**
     * OTP code generation
     * @param secret the cryptographically-secure secret
     * @param digits how long (the number of digits) is the OTP
     * @param timeStep the validity of otp
     * @return [OTPInfo] holding the code and validity duration.
     */
    fun generateCode(
        secret: ByteArray
    ): OTPInfo {
        val generator = TimeBasedOneTimePasswordGenerator(secret, otpConfig)
        val otpCode = generator.generate()

        val counter = generator.counter()
        val endEpochMillis = generator.timeslotStart(counter + 2) - 1
        return OTPInfo(
            otp = otpCode,
            validity = (endEpochMillis - Instant.now().toEpochMilli()).milliseconds
        )
    }


    /**
     * @param secret the secret that was used to generate the otp code.
     * @param code the code to check whether it is valid or not
     * @return true if the code is valid, false otherwise
     */
    fun isValid(
        secret: ByteArray,
        code: String
    ): Boolean {
        val generator = TimeBasedOneTimePasswordGenerator(secret, otpConfig)

        val validInstants = listOf(
            Instant.now(),
            Instant.now() - (halfTimeStep.toJavaDuration())
        )

        return validInstants.any { instant ->
            generator.isValid(code = code, instant = instant)
        }
    }
}

data class OTPInfo(
    val otp: String,
    val validity: Duration
)