package com.safehill.safehillclient.model.user

import com.safehill.kclient.base64.decodeBase64
import com.safehill.kclient.base64.encodeBase64
import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.SafehillPrivateKey
import com.safehill.kclient.models.SafehillPublicKey
import com.safehill.kclient.models.users.LocalUser
import kotlinx.serialization.Serializable
import java.security.KeyPair

private const val KEY_CHAIN_PREFIX = "com.gf.safehill"

@Serializable
data class UserQrData(
    val shUser: UserKeyData,
    val keychainPrefix: String
)

@Serializable
data class UserKeyData(
    val privateKeyData: String,
    val privateSignatureData: String
)

fun LocalUser.toUserQrData() = UserQrData(
    shUser = UserKeyData(
        privateKeyData = this.shUser.key.private.encoded.encodeBase64().decodeToString(),
        privateSignatureData = this.shUser.signature.private.encoded.encodeBase64().decodeToString()
    ),
    keychainPrefix = KEY_CHAIN_PREFIX
)

fun UserQrData.toLocalUser(): LocalUser {
    val privateKey = SafehillPrivateKey.from(
        this.shUser.privateKeyData.toByteArray().decodeBase64()
    )
    val signaturePrivateKey = SafehillPrivateKey.from(
        this.shUser.privateSignatureData.toByteArray().decodeBase64()
    )
    return LocalUser(
        shUser = LocalCryptoUser(
            key = KeyPair(
                SafehillPublicKey.derivePublicKeyFrom(privateKey),
                privateKey
            ),
            signature = KeyPair(
                SafehillPublicKey.derivePublicKeyFrom(signaturePrivateKey),
                signaturePrivateKey
            )
        )
    )
}