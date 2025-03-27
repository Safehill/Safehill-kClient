package com.safehill.safehillclient.data.user.model

import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import java.security.PublicKey

data class AppUser(
    val userName: String,
    val identifier: String,
    val publicKey: PublicKey,
    val publicSignature: PublicKey
)

fun ServerUser.toAppUser() = AppUser(
    userName = this.name,
    identifier = this.identifier,
    publicKey = this.publicKey,
    publicSignature = this.publicSignature
)

fun AppUser.toServerUser() = RemoteUser(
    identifier = this.identifier,
    name = this.userName,
    publicKeyData = this.publicKey.encoded,
    publicSignatureData = this.publicSignature.encoded
)

fun getRandomAppUser(name: String) = LocalUser(LocalCryptoUser()).apply {
    this.name = name
}.toAppUser()