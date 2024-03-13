package com.safehill.mock

import com.safehill.kclient.models.user.SHLocalUserInterface
import com.safehill.kclient.network.ServerProxyInterface
import com.safehill.kcrypto.models.SHLocalCryptoUser
import java.security.PublicKey

class SHLocalUserSpy: SHLocalUserInterface {
    override val authToken: String
        get() = TODO("Not yet implemented")
    override val publicSignatureData: ByteArray
        get() = TODO("Not yet implemented")
    override val publicKeyData: ByteArray
        get() = TODO("Not yet implemented")
    override val shUser: SHLocalCryptoUser
        get() = TODO("Not yet implemented")
    override val serverProxy: ServerProxyInterface
        get() = TODO("Not yet implemented")
    override val identifier: String
        get() = TODO("Not yet implemented")
    override val name: String
        get() = TODO("Not yet implemented")
    override val publicKey: PublicKey
        get() = TODO("Not yet implemented")
    override val publicSignature: PublicKey
        get() = TODO("Not yet implemented")
}
