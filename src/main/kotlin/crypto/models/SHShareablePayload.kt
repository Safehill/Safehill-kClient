package crypto.models

data class SHShareablePayload(
    val ephemeralPublicKeyData: ByteArray,
    val ciphertext: ByteArray,
    val signature: ByteArray,
    val recipient: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SHShareablePayload

        if (!ephemeralPublicKeyData.contentEquals(other.ephemeralPublicKeyData)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!signature.contentEquals(other.signature)) return false
        if (recipient != other.recipient) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ephemeralPublicKeyData.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        result = 31 * result + recipient.hashCode()
        return result
    }
}