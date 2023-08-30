package com.safehill.kclient.api

import com.safehill.kclient.api.dtos.SHAuthResponse
import com.safehill.kclient.models.SHLocalUser
import com.safehill.kclient.models.SHServerUser
import com.safehill.kcrypto.models.SHLocalCryptoUser
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

class SHHTTPAPITests {

    @Test
    fun testUserAuthCrud() {
        runBlocking {
            val cryptoUser = SHLocalCryptoUser()
            val localUser = SHLocalUser(cryptoUser)

            var serverUser: SHServerUser? = null
            var error: Exception? = null
            val createJob = launch {
                try {
                    serverUser = SHHTTPAPI(localUser).createUser("newUser")
                } catch (err: Exception) {
                    error = err
                }
            }
            createJob.join()

            serverUser?.let {
                println("Created new user with name: ${it.name}")
                localUser.name = it.name
            } ?: run {
                error?.let {
                    println("error: $it")
                    throw it
                }
            }

            var authResponse: SHAuthResponse? = null
            val authJob = launch {
                try {
                    authResponse = SHHTTPAPI(localUser).signIn(localUser.name)
                } catch (err: Exception) {
                    error = err
                }
            }
            authJob.join()

            authResponse?.let {
                println("Auth token: ${it.bearerToken}")
                localUser.authenticate(it.user, it.bearerToken)
            } ?: run {
                error?.let {
                    println("error: $it")
                    throw it
                }
            }

            val deleteJob = launch {
                try {
                    SHHTTPAPI(localUser).deleteAccount()
                } catch (err: Exception) {
                    error = err
                }
            }
            deleteJob.join()
        }
    }

    @Test
    fun testEncryptDecryptSharedSecret() {

    }
}