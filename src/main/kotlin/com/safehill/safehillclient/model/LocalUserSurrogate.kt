package com.safehill.safehillclient.model

import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.SafehillPrivateKey
import com.safehill.kclient.models.SafehillPublicKey
import com.safehill.kclient.models.serde.Base64DataSerializer
import com.safehill.kclient.models.users.LocalUser
import kotlinx.serialization.Serializable
import java.security.KeyPair

@Serializable
class LocalUserSurrogate(
    val name: String,
    @Serializable(with = Base64DataSerializer::class) val publicKeyData: ByteArray,
    @Serializable(with = Base64DataSerializer::class) val privateKeyData: ByteArray,
    @Serializable(with = Base64DataSerializer::class) val publicSignatureData: ByteArray,
    @Serializable(with = Base64DataSerializer::class) val privateSignatureData: ByteArray
)

fun LocalUser.toSurrogate(): LocalUserSurrogate {
    return LocalUserSurrogate(
        name = this.name,
        publicKeyData = this.shUser.key.public.encoded,
        privateKeyData = this.shUser.key.private.encoded,
        publicSignatureData = this.shUser.signature.public.encoded,
        privateSignatureData = this.shUser.signature.private.encoded
    )
}

fun LocalUserSurrogate.toLocalUser(): LocalUser {
    val surrogate = this
    return LocalUser(
        LocalCryptoUser(
            KeyPair(
                SafehillPublicKey.from(surrogate.publicKeyData),
                SafehillPrivateKey.from(surrogate.privateKeyData)
            ), KeyPair(
                SafehillPublicKey.from(surrogate.publicSignatureData),
                SafehillPrivateKey.from(surrogate.privateSignatureData)
            )
        )
    ).apply {
        this.name = surrogate.name
    }
}