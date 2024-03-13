package com.safehill.kclient.api

import com.safehill.kclient.api.dtos.SHAuthResponseDTO
import com.safehill.kclient.api.dtos.SHAssetOutputDTO
import com.safehill.kclient.models.*
import com.safehill.kclient.api.dtos.SHSendCodeToUserRequestDTO
import com.safehill.kclient.network.ServerProxyInterface
import com.safehill.kcrypto.models.SHKeyPair
import com.safehill.kcrypto.models.SHLocalCryptoUser
import com.safehill.mock.ServerProxySpy
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.Date
import kotlin.random.Random

class SafehillApiImplTests {

    private suspend fun createUserOnServer(coroutineScope: CoroutineScope, user: SHLocalUser? = null): SHLocalUser {
        val localUser: SHLocalUser = user ?: run {
            val cryptoUser = SHLocalCryptoUser()
            SHLocalUser(cryptoUser) { ServerProxySpy() }
        }

        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val newUserName = (1..20)
            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
            .joinToString("")

        var serverUser: SHServerUser? = null
        var error: Exception? = null
        val createJob = coroutineScope.launch {
            try {
                serverUser = SafehillApiImpl(localUser).createUser(newUserName)
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
        var authResponse: SHAuthResponseDTO? = null

        val authJob = coroutineScope.launch {
            try {
                authResponse = SafehillApiImpl(localUser).signIn()
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
                assert(!it.metadata.isPhoneNumberVerified)
                println("Auth token: ${it.bearerToken}")
                localUser.authenticate(it.user, it.bearerToken)
            }
        }
    }

    private suspend fun deleteUser(coroutineScope: CoroutineScope, localUser: SHLocalUser) {
        var error: Exception? = null

        val deleteJob = coroutineScope.launch {
            try {
                SafehillApiImpl(localUser).deleteAccount()
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

    private suspend fun deleteAssets(coroutineScope: CoroutineScope, localUser: SHLocalUser,assets: List<SHAssetOutputDTO>) {
        var error: Exception? = null

        val deleteJob = coroutineScope.launch {
            try {
                SafehillApiImpl(localUser).deleteAssets(assets.map { it.globalIdentifier })
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
    fun testUserAuthCRUD() {
        runBlocking {
            val user = createUserOnServer(this)
            authenticateUser(this, user)

            var error: Exception? = null
            var getJob = launch {
                try {
                    val users = SafehillApiImpl(user).getUsers(listOf(user.identifier))
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

            val newPhoneNumber = "+11234567890"

            val updateJob = launch {
                try {
                    val updatedUser = SafehillApiImpl(user).updateUser(
                        name = null,
                        phoneNumber = newPhoneNumber,
                        email = null
                    )
                    assert(updatedUser.identifier == user.identifier)
                    assert(updatedUser.name == user.name)
                } catch (err: Exception) {
                    error = err
                }
            }
            updateJob.join()

            error?.let {
                println("error: $it")
                deleteUser(this, user)
                throw it
            }

            val authJob = launch {
                try {
                    val authResponse = SafehillApiImpl(user).signIn()
                    assert(authResponse.metadata.isPhoneNumberVerified)
                } catch (err: Exception) {
                    error = err
                }
            }
            authJob.join()

            error?.let {
                println("error: $it")
                deleteUser(this, user)
                throw it
            }

            getJob = launch {
                try {
                    val users = SafehillApiImpl(user).getUsers(listOf(user.identifier))
                    assert(users.isNotEmpty())
                    assert(users.count() == 1)
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

            deleteUser(this, user)
        }
    }

    @Test
    fun testAssetAndDescriptorCRUD() {
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
            val user = createUserOnServer(this)
            authenticateUser(this, user)

            val api = SafehillApiImpl(user)

            var createdAsset: SHAssetOutputDTO? = null
            var error: Exception? = null
            val createJob = launch {
                try {
                    createdAsset = api.create(
                        listOf(encryptedAsset),
                        groupId,
                        null
                    ).first()
                } catch (err: Exception) {
                    error = err
                }
            }
            createJob.join()

            error?.let {
                deleteUser(this, user)
                println("error: $it")
                throw it
            }

            var descriptors: List<SHAssetDescriptor> = emptyList()
            val getDescriptorJob = launch {
                try {
                    descriptors = api.getAssetDescriptors()
                } catch (err: Exception) {
                    error = err
                }
            }
            getDescriptorJob.join()

            error?.let {
                deleteAssets(this, user, listOf(createdAsset!!))
                deleteUser(this, user)
                println("error: $it")
                throw it
            }

            assert(descriptors.size == 1)

            deleteAssets(this, user, listOf(createdAsset!!))
            deleteUser(this, user)
        }
    }

    @Test
    fun testUnauthorizedGetUsers() {
        val cryptoUser = SHLocalCryptoUser()
        val localUser = SHLocalUser(cryptoUser) { ServerProxySpy() }
        val api = SafehillApiImpl(localUser)

        runBlocking {
            try {
                api.getUsers(listOf(localUser.shUser.identifier)).firstOrNull()
            } catch (e: SafehillHttpException) {
                assert(e.statusCode == SafehillHttpStatusCode.UnAuthorized)
            }

            createUserOnServer(this, localUser)
            authenticateUser(this, localUser)

            api.getUsers(listOf(localUser.shUser.identifier)).firstOrNull()

            // Invalid auth token
            localUser.authToken = ""

            try {
                api.getUsers(listOf(localUser.shUser.identifier)).firstOrNull()
            } catch (e: SafehillHttpException) {
                assert(e.statusCode == SafehillHttpStatusCode.UnAuthorized)
            }
        }
    }

    @Test
    fun testAuthenticateNonExistingUser() {
        val localUser = SHLocalUser(SHLocalCryptoUser()) { ServerProxySpy() }
        val api = SafehillApiImpl(localUser)

        runBlocking {
            try {
                api.signIn()
            } catch (e: SafehillHttpException) {
                assert(e.statusCode == SafehillHttpStatusCode.NotFound)
            }
        }
    }

    @Test
    fun testSendCodeToUser() {
        runBlocking {
            val user = createUserOnServer(this)
            authenticateUser(this, user)

            var error: Exception? = null
            val api = SafehillApiImpl(user)

            try {
                api.sendCodeToUser(1, 4151234567, "12345", SHSendCodeToUserRequestDTO.Medium.SMS)
            } catch (e: Exception) {
                error = e
            }

            try {
                api.sendCodeToUser(1, 4151234567, "12345", SHSendCodeToUserRequestDTO.Medium.Phone)
            } catch (e: Exception) {
                error = e
            }

            error?.let {
                deleteUser(this, user)
                println("error: $it")
                throw it
            }
        }
    }
}