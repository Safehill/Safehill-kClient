package com.safehill.kclient.api

import com.safehill.kclient.api.dtos.SHAuthResponse
import com.safehill.kclient.api.dtos.SHServerAsset
import com.safehill.kclient.models.*
import com.safehill.kcrypto.models.SHKeyPair
import com.safehill.kcrypto.models.SHLocalCryptoUser
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.Date
import kotlin.random.Random

class SHHTTPAPITests {

    private suspend fun createNewUser(coroutineScope: CoroutineScope): SHLocalUser {
        val cryptoUser = SHLocalCryptoUser()
        val localUser = SHLocalUser(cryptoUser)

        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val newUserName = (1..20)
            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
            .joinToString("")

        var serverUser: SHServerUser? = null
        var error: Exception? = null
        val createJob = coroutineScope.launch {
            try {
                serverUser = SHHTTPAPI(localUser).createUser(newUserName)
            } catch (err: Exception) {
                error = err
            }
        }
        createJob.join()

        error?.let {
            println("error: $it")
            throw it
        } ?: run {
            serverUser?.let {
                println("Created new user with name: ${it.name}")
                localUser.name = it.name
            }
        }

        return localUser
    }

    private suspend fun authenticateUser(coroutineScope: CoroutineScope, localUser: SHLocalUser) {
        var error: Exception? = null
        var authResponse: SHAuthResponse? = null

        val authJob = coroutineScope.launch {
            try {
                authResponse = SHHTTPAPI(localUser).signIn(localUser.name)
            } catch (err: Exception) {
                error = err
            }
        }
        authJob.join()

        error?.let {
            println("error: $it")
            throw it
        } ?: run {
            authResponse?.let {
                println("Auth token: ${it.bearerToken}")
                localUser.authenticate(it.user, it.bearerToken)
            }
        }
    }

    private suspend fun deleteUser(coroutineScope: CoroutineScope, localUser: SHLocalUser) {
        var error: Exception? = null

        val deleteJob = coroutineScope.launch {
            try {
                SHHTTPAPI(localUser).deleteAccount()
            } catch (err: Exception) {
                error = err
            }
        }
        deleteJob.join()

        error?.let {
            println("error: $it")
            throw it
        }
    }

    @Test
    fun testUserAuthCrud() {
        runBlocking {
            val user = createNewUser(this)
            authenticateUser(this, user)

            var error: Exception? = null
            val getJob = launch {
                try {
                    val users = SHHTTPAPI(user).getUsers(listOf(user.identifier))
                    assert(users.isNotEmpty())
                    val retrievedUser = users[0]
                    assert(retrievedUser.identifier == user.identifier)
                    assert(retrievedUser.name == user.name)
                    assert(Base64.getEncoder().encodeToString(retrievedUser.publicKeyData) == Base64.getEncoder().encodeToString(user.publicKeyData))
                    assert(Base64.getEncoder().encodeToString(retrievedUser.publicSignatureData) == Base64.getEncoder().encodeToString(user.publicSignatureData))
                } catch (err: Exception) {
                    error = err
                }
            }
            getJob.join()

            error?.let {
                println("error: $it")
                deleteUser(this, user)
                throw it
            }

            deleteUser(this, user)
        }
    }

    @Test
    fun testCreateAsset() {
        val groupId = "sampleGroupId"
        val assetKey = SHKeyPair.generate()
        val assetSignature = SHKeyPair.generate()
        val encryptedAsset = SHEncryptedAssetImpl(
            globalIdentifier = "globalIdentifier",
            localIdentifier = null,
            creationDate = Date(0),
            encryptedVersions = mapOf(Pair(
                SHAssetQuality.LowResolution,
                SHEncryptedAssetVersionImpl(
                    quality = SHAssetQuality.LowResolution,
                    encryptedData = "encryptedData".toByteArray(),
                    encryptedSecret = "encryptedData".toByteArray(),
                    publicKeyData = assetKey.public.encoded,
                    publicSignatureData = assetSignature.public.encoded
                )
            ))
        )

        runBlocking {
            val user = createNewUser(this)
            authenticateUser(this, user)

            var createdAsset: SHServerAsset? = null
            var error: Exception? = null
            val getJob = launch {
                try {
                    createdAsset = SHHTTPAPI(user).create(
                        listOf(encryptedAsset),
                        groupId,
                        null
                    ).first()
                } catch (err: Exception) {
                    deleteUser(this, user)
                    error = err
                }
            }
            getJob.join()

            error?.let {
                println("error: $it")
                throw it
            }

            deleteUser(this, user)
        }
    }
}