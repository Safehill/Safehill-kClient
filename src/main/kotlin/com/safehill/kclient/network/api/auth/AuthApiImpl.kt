package com.safehill.kclient.network.api.auth

import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.base64.base64EncodedString
import com.safehill.kclient.models.RemoteCryptoUser
import com.safehill.kclient.models.dtos.AuthChallengeRequestDTO
import com.safehill.kclient.models.dtos.AuthChallengeResponseDTO
import com.safehill.kclient.models.dtos.AuthResolvedChallengeDTO
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.UserInputDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.api.BaseOpenApi
import com.safehill.kclient.network.api.postRequestForObjectResponse
import com.safehill.kcrypto.models.ShareablePayload
import io.ktor.client.HttpClient
import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64

class AuthApiImpl(
    private val baseOpenApi: BaseOpenApi
) : AuthApi, BaseOpenApi by baseOpenApi {

    constructor(httpClient: HttpClient) : this(
        object : BaseOpenApi {
            override val client: HttpClient = httpClient
        }
    )

    override suspend fun signIn(user: LocalUser): AuthResponseDTO {
        val authRequestBody = AuthChallengeRequestDTO(
            identifier = user.identifier,
        )

        val authChallenge: AuthChallengeResponseDTO = postRequestForObjectResponse(
            endPoint = "/signin/challenge/start",
            request = authRequestBody
        )

        val solvedChallenge = this.solveChallenge(authChallenge, user)

        return postRequestForObjectResponse(
            endPoint = "/signin/challenge/verify",
            request = solvedChallenge
        )
    }

    @Throws
    fun solveChallenge(
        authChallenge: AuthChallengeResponseDTO,
        requestor: LocalUser
    ): AuthResolvedChallengeDTO {
        val serverCrypto = RemoteCryptoUser(
            Base64.getDecoder().decode(authChallenge.publicKey),
            Base64.getDecoder().decode(authChallenge.publicSignature)
        )
        val encryptedChallenge = ShareablePayload(
            ephemeralPublicKeyData = Base64.getDecoder()
                .decode(authChallenge.ephemeralPublicKey),
            ciphertext = Base64.getDecoder().decode(authChallenge.challenge),
            signature = Base64.getDecoder().decode(authChallenge.ephemeralPublicSignature),
            null
        )

        val decryptedChallenge = SafehillCypher.decrypt(
            sealedMessage = encryptedChallenge,
            encryptionKey = requestor.shUser.key,
            signedBy = serverCrypto.publicSignature,
            iv = authChallenge.iv?.let { Base64.getDecoder().decode(it) },
            protocolSalt = Base64.getDecoder().decode(authChallenge.protocolSalt)
        )
        val signatureForChallenge = requestor.shUser.sign(decryptedChallenge)
        val md = MessageDigest.getInstance("SHA-512")
        val digest512 = md.digest(decryptedChallenge)
        val signatureForDigest = requestor.shUser.sign(digest512)

        return AuthResolvedChallengeDTO(
            userIdentifier = requestor.identifier,
            signedChallenge = Base64.getEncoder().encodeToString(signatureForChallenge),
            digest = Base64.getEncoder().encodeToString(digest512),
            signedDigest = Base64.getEncoder().encodeToString(signatureForDigest)
        )
    }

    @Throws
    override suspend fun createUser(
        name: String,
        identifier: String,
        publicKey: PublicKey,
        signature: PublicKey
    ): ServerUser {
        val requestBody = UserInputDTO(
            identifier = identifier,
            publicKey = publicKey.encoded.base64EncodedString(),
            publicSignature = signature.encoded.base64EncodedString(),
            name = name
        )
        return postRequestForObjectResponse<UserInputDTO, RemoteUser>(
            endPoint = "/users/create",
            request = requestBody
        )
    }

}