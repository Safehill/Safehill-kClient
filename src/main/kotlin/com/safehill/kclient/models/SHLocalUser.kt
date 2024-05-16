package com.safehill.kclient.models

import com.safehill.kclient.api.dtos.SHAuthResponseDTO
import com.safehill.kclient.errors.SHBackgroundOperationError
import com.safehill.kclient.errors.SHLocalUserError
import com.safehill.kcrypto.models.SHCryptoUser
import com.safehill.kcrypto.models.SHLocalCryptoUser
import com.safehill.kcrypto.models.SHShareablePayload
import com.safehill.kcrypto.models.SHUserContext
import java.security.PublicKey
import java.util.Base64

class SHLocalUser(
    var shUser: SHLocalCryptoUser,
    val maybeEncryptionProtocolSalt: ByteArray? = null
) : SHServerUser {

    override val identifier: String
        get() = this.shUser.identifier

    override var name: String = ""

    override val publicKey: PublicKey
        get() = this.shUser.publicKey

    override val publicSignature: PublicKey
        get() = this.shUser.publicSignature

    override val publicKeyData: ByteArray
        get() = this.shUser.publicKeyData

    override val publicSignatureData: ByteArray
        get() = this.shUser.publicSignatureData

    var authToken: String? = null
    var encryptionSalt: ByteArray = byteArrayOf()

    fun decrypt(
        asset: SHEncryptedAsset,
        quality: SHAssetQuality,
        receivedFromUser: SHServerUser
    ): SHDecryptedAsset {
        val version = asset.encryptedVersions[quality]
            ?: throw SHBackgroundOperationError.FatalError("No such version ${quality.name} for asset=${asset.globalIdentifier}")

        val sharedSecret = SHShareablePayload(
            ephemeralPublicKeyData = version.publicKeyData,
            ciphertext = version.encryptedSecret,
            signature = version.publicSignatureData
        )

        val decryptedData = decrypt(
            data = version.encryptedData,
            encryptedSecret = sharedSecret,
            receivedFrom = receivedFromUser
        )

        return SHGenericDecryptedAsset(
            globalIdentifier = asset.globalIdentifier,
            localIdentifier = asset.localIdentifier,
            decryptedVersions = mutableMapOf(quality to decryptedData),
            creationDate = asset.creationDate
        )

    }

    fun decrypt(
        data: ByteArray,
        encryptedSecret: SHShareablePayload,
        receivedFrom: SHServerUser
    ): ByteArray {
        val salt = maybeEncryptionProtocolSalt ?: throw SHLocalUserError.MissingProtocolSalt

        return SHUserContext(shUser)
            .decrypt(
                data,
                encryptedSecret,
                salt,
                receivedFrom
            )

    }

    private fun updateUserDetails(given: SHServerUser?) {
        given?.let {
            this.name = it.name
        } ?: run {
            this.name = ""
        }
    }

    fun authenticate(user: SHServerUser, authResponseDTO: SHAuthResponseDTO) {
        this.updateUserDetails(user)
        this.authToken = authResponseDTO.bearerToken
        this.encryptionSalt = Base64.getDecoder().decode(authResponseDTO.encryptionProtocolSalt)
    }

    fun deauthenticate() {
        this.authToken = null
    }

    fun shareable(
        data: ByteArray,
        with: SHCryptoUser,
        protocolSalt: ByteArray
    ): SHShareablePayload {
        return SHUserContext(this.shUser).shareable(data, with, protocolSalt)
    }

    fun decrypted(
        data: ByteArray,
        encryptedSecret: SHShareablePayload,
        protocolSalt: ByteArray,
        receivedFrom: SHCryptoUser
    ): ByteArray {
        return SHUserContext(this.shUser).decrypt(
            data,
            encryptedSecret,
            protocolSalt,
            receivedFrom
        )
    }

    fun regenerateKeys() {
        this.deauthenticate()
        this.shUser = SHLocalCryptoUser()
    }
}
