package com.safehill.kclient.models.users

import com.safehill.kclient.errors.BackgroundOperationError
import com.safehill.kclient.errors.LocalUserError
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.BearerToken
import com.safehill.kcrypto.models.CryptoUser
import com.safehill.kcrypto.models.LocalCryptoUser
import com.safehill.kcrypto.models.SHUserContext
import com.safehill.kcrypto.models.ShareablePayload
import java.security.PublicKey
import java.util.Base64

class LocalUser(
    var shUser: LocalCryptoUser,
    private val maybeEncryptionProtocolSalt: ByteArray? = null
) : ServerUser {

    override val identifier: UserIdentifier
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

    var authToken: BearerToken? = null
    var encryptionSalt: ByteArray = byteArrayOf()

    fun decrypt(
        asset: EncryptedAsset,
        quality: AssetQuality,
        receivedFromUser: ServerUser
    ): DecryptedAsset {
        val version = asset.encryptedVersions[quality]
            ?: throw BackgroundOperationError.FatalError("No such version ${quality.name} for asset=${asset.globalIdentifier}")

        val sharedSecret = ShareablePayload(
            ephemeralPublicKeyData = version.publicKeyData,
            ciphertext = version.encryptedSecret,
            signature = version.publicSignatureData,
        )

        val decryptedData = decrypt(
            data = version.encryptedData,
            encryptedSecret = sharedSecret,
            receivedFrom = receivedFromUser
        )

        return DecryptedAsset(
            globalIdentifier = asset.globalIdentifier,
            localIdentifier = asset.localIdentifier,
            decryptedVersions = mutableMapOf(quality to decryptedData),
            creationDate = asset.creationDate
        )

    }

    fun decrypt(
        data: ByteArray,
        encryptedSecret: ShareablePayload,
        receivedFrom: ServerUser
    ): ByteArray {
        val salt = maybeEncryptionProtocolSalt ?: throw LocalUserError.MissingProtocolSalt

        return SHUserContext(shUser)
            .decrypt(
                data,
                encryptedSecret,
                salt,
                receivedFrom
            )

    }

    private fun updateUserDetails(given: ServerUser?) {
        given?.let {
            this.name = it.name
        } ?: run {
            this.name = ""
        }
    }

    fun authenticate(user: ServerUser, authResponseDTO: AuthResponseDTO) {
        this.updateUserDetails(user)
        this.authToken = authResponseDTO.bearerToken
        this.encryptionSalt = Base64.getDecoder().decode(authResponseDTO.encryptionProtocolSalt)
    }

    fun deauthenticate() {
        this.authToken = null
    }

    fun shareable(
        data: ByteArray,
        with: CryptoUser,
        protocolSalt: ByteArray
    ): ShareablePayload {
        return SHUserContext(this.shUser).shareable(data, with, protocolSalt)
    }

    fun decrypted(
        data: ByteArray,
        encryptedSecret: ShareablePayload,
        protocolSalt: ByteArray,
        receivedFrom: CryptoUser
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
        this.shUser = LocalCryptoUser()
    }
}
