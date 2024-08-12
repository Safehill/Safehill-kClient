package com.safehill.kclient.api

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.SendCodeToUserRequestDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kclient.network.remote.RemoteServer
import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.SafehillKeyPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.Date
import kotlin.random.Random
import kotlin.test.assertNotNull

class RemoteServerTests {

    private suspend fun createUserOnServer(
        coroutineScope: CoroutineScope,
        user: LocalUser? = null
    ): LocalUser {
        val localUser: LocalUser = user ?: run {
            val cryptoUser = LocalCryptoUser()
            LocalUser(cryptoUser)
        }

        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val newUserName = (1..20)
            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
            .joinToString("")

        var serverUser: ServerUser? = null
        var error: Exception? = null
        val createJob = coroutineScope.launch {
            try {
                serverUser = RemoteServer(localUser).createUser(newUserName)
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

    private suspend fun authenticateUser(coroutineScope: CoroutineScope, localUser: LocalUser) {
        var error: Exception? = null
        var authResponse: AuthResponseDTO? = null

        val authJob = coroutineScope.launch {
            try {
                authResponse = RemoteServer(localUser).signIn()
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
                localUser.authenticate(it.user, it)
            }
        }
    }

    private suspend fun deleteUser(coroutineScope: CoroutineScope, localUser: LocalUser) {
        var error: Exception? = null

        val deleteJob = coroutineScope.launch {
            try {
                RemoteServer(localUser).deleteAccount()
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

    private suspend fun deleteAssets(
        coroutineScope: CoroutineScope,
        localUser: LocalUser,
        assets: List<AssetOutputDTO>
    ) {
        var error: Exception? = null

        val deleteJob = coroutineScope.launch {
            try {
                RemoteServer(localUser).deleteAssets(assets.map { it.globalIdentifier })
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

    @Disabled
    @Test
    fun testUserAuthCRUD() {
        runBlocking {
            val user = createUserOnServer(this)
            authenticateUser(this, user)

            var error: Exception? = null
            var getJob = launch {
                try {
                    val users = RemoteServer(user).getUsers(listOf(user.identifier))
                    assert(users.isNotEmpty())
                    val retrievedUser = users[user.identifier]
                    assertNotNull(retrievedUser)
                    assert(retrievedUser.identifier == user.identifier)
                    assert(retrievedUser.name == user.name)
                    assert(
                        Base64.getEncoder()
                            .encodeToString(retrievedUser.publicKeyData) == Base64.getEncoder()
                            .encodeToString(user.publicKeyData)
                    )
                    assert(
                        Base64.getEncoder()
                            .encodeToString(retrievedUser.publicSignatureData) == Base64.getEncoder()
                            .encodeToString(user.publicSignatureData)
                    )
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
                    val updatedUser = RemoteServer(user).updateUser(
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
                    val authResponse = RemoteServer(user).signIn()
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
                    val users = RemoteServer(user).getUsers(listOf(user.identifier))
                    assert(users.isNotEmpty())
                    assert(users.count() == 1)
                    val retrievedUser = users[user.identifier]
                    assertNotNull(retrievedUser)
                    assert(retrievedUser.identifier == user.identifier)
                    assert(retrievedUser.name == user.name)
                    assert(
                        Base64.getEncoder()
                            .encodeToString(retrievedUser.publicKeyData) == Base64.getEncoder()
                            .encodeToString(user.publicKeyData)
                    )
                    assert(
                        Base64.getEncoder()
                            .encodeToString(retrievedUser.publicSignatureData) == Base64.getEncoder()
                            .encodeToString(user.publicSignatureData)
                    )
                } catch (err: Exception) {
                    error = err
                }
            }
            getJob.join()

            deleteUser(this, user)
        }
    }

    @Disabled
    @Test
    fun testAssetAndDescriptorCRUD() {
        val groupId = "sampleGroupId"
        val assetKey = SafehillKeyPair.generate()
        val assetSignature = SafehillKeyPair.generate()
        val encryptedAsset = EncryptedAsset(
            globalIdentifier = "globalIdentifier",
            localIdentifier = null,
            creationDate = Date(0).toInstant(),
            encryptedVersions = mapOf(
                Pair(
                    AssetQuality.LowResolution,
                    EncryptedAssetVersion(
                        quality = AssetQuality.LowResolution,
                        encryptedData = "encryptedData".toByteArray(),
                        encryptedSecret = "encryptedData".toByteArray(),
                        publicKeyData = assetKey.public.encoded,
                        publicSignatureData = assetSignature.public.encoded
                    )
                )
            )
        )

        runBlocking {
            val user = createUserOnServer(this)
            authenticateUser(this, user)

            val api = RemoteServer(user)

            var createdAsset: AssetOutputDTO? = null
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

            var descriptors: List<AssetDescriptor> = emptyList()
            var getDescriptorJob = launch {
                try {
                    descriptors = api.getAssetDescriptors(after = null)
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

            getDescriptorJob = launch {
                try {
                    descriptors = api.getAssetDescriptors(
                        assetGlobalIdentifiers = listOf(createdAsset!!.globalIdentifier),
                        groupIds = null,
                        after = null
                    )
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

    @Disabled
    @Test
    fun testUnauthorizedGetUsers() {
        val cryptoUser = LocalCryptoUser()
        val localUser = LocalUser(cryptoUser)
        val api = RemoteServer(localUser)

        runBlocking {
            val userId = localUser.shUser.identifier
            try {
                api.getUsers(listOf(userId))[userId]
            } catch (e: SafehillError) {
                assert(e is SafehillError.ClientError.Unauthorized)
            }

            createUserOnServer(this, localUser)
            authenticateUser(this, localUser)

            api.getUsers(listOf(userId))[userId]

            // Invalid auth token
            localUser.authToken = ""

            try {
                api.getUsers(listOf(userId))[userId]
            } catch (e: SafehillError) {
                assert(e is SafehillError.ClientError.Unauthorized)
            }
        }
    }

    @Disabled
    @Test
    fun testAuthenticateNonExistingUser() {
        val localUser = LocalUser(LocalCryptoUser())
        val api = RemoteServer(localUser)

        runBlocking {
            try {
                api.signIn()
            } catch (e: SafehillError) {
                assert(e is SafehillError.ClientError.NotFound)
            }
        }
    }

    @Disabled
    @Test
    fun testSendCodeToUser() {
        runBlocking {
            val user = createUserOnServer(this)
            authenticateUser(this, user)

            var error: Exception? = null
            val api = RemoteServer(user)

            try {
                api.sendCodeToUser(1, 4151234567, "12345", SendCodeToUserRequestDTO.Medium.SMS)
            } catch (e: Exception) {
                error = e
            }

            try {
                api.sendCodeToUser(1, 4151234567, "12345", SendCodeToUserRequestDTO.Medium.Phone)
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