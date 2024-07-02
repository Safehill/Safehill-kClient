package com.safehill.kclient.tasks

// TODO(@diken) TO UNCOMMENT

import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.ServerProxyImpl
import com.safehill.kclient.network.local.LocalServerInterfaceImpl
import com.safehill.kclient.network.remote.RemoteServer
import com.safehill.kclient.tasks.inbound.DownloadOperationListener
import com.safehill.kclient.tasks.inbound.RemoteDownloadOperation
import com.safehill.kcrypto.models.LocalCryptoUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test

//class RemoteDownloadTaskTests {
//
//    private suspend fun createUserOnServer(
//        coroutineScope: CoroutineScope,
//        user: LocalUser? = null
//    ): LocalUser {
//        val localUser: LocalUser = user ?: run {
//            val cryptoUser = LocalCryptoUser()
//            LocalUser(cryptoUser)
//        }
//
//        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
//        val newUserName = (1..20)
//            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
//            .joinToString("")
//
//        var serverUser: ServerUser? = null
//        var error: Exception? = null
//        val createJob = coroutineScope.launch {
//            try {
//                serverUser = RemoteServer(localUser).createUser(newUserName)
//            } catch (err: Exception) {
//                error = err
//            }
//        }
//        createJob.join()
//
//        error?.let {
//            println("error: $it")
//            throw it
//        } ?: run {
//            serverUser?.let {
//                println("Created new user with name: ${it.name}")
//                localUser.name = it.name
//            }
//        }
//
//        return localUser
//    }
//
//    private suspend fun authenticateUser(coroutineScope: CoroutineScope, localUser: LocalUser) {
//        var error: Exception? = null
//        var authResponse: AuthResponseDTO? = null
//
//        val authJob = coroutineScope.launch {
//            try {
//                authResponse = RemoteServer(localUser).signIn()
//            } catch (err: Exception) {
//                error = err
//            }
//        }
//        authJob.join()
//
//        error?.let {
//            println("error: $it")
//            throw it
//        } ?: run {
//            authResponse?.let {
//                assert(!it.metadata.isPhoneNumberVerified)
//                println("Auth token: ${it.bearerToken}")
//                localUser.authenticate(it.user, it)
//            }
//        }
//    }
//
//    class DummyRemoteDownloadListener : DownloadOperationListener {
//        override fun received(
//            assetDescriptors: List<AssetDescriptor>,
//            referencingUsers: Map<UserIdentifier, ServerUser>,
//        ) {
//            println("received ${assetDescriptors.size} descriptors")
//        }
//
//        override fun fetched(decryptedAsset: DecryptedAsset) {
//            println("fetched low resolution asset with global identifier ${decryptedAsset.globalIdentifier}")
//        }
//    }
//
//    @Test
//    fun testRemoteDownload() = runBlocking {
//        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
//        val remoteDownloadProcessor =
//            BackgroundTaskProcessor<RemoteDownloadOperation>(coroutineScope)
//
//        val cryptoUser = LocalCryptoUser()
//        val localUser = LocalUser(cryptoUser)
//        val serverUser = createUserOnServer(this)
//        authenticateUser(this, serverUser)
//
//        val localServer = LocalServerInterfaceImpl()
//        val remoteServer = RemoteServer(localUser)
//        val serverProxy = ServerProxyImpl(localServer, remoteServer, serverUser)
//
//        val userController = UserController(serverProxy)
//
//
//        val remoteDownloadOperation = RemoteDownloadOperation(
//            serverProxy, listOf(DummyRemoteDownloadListener()), userController
//        )
//
//        remoteDownloadProcessor.addTask(remoteDownloadOperation)
//
//        delay(500)
//
//        coroutineScope.cancel()
//    }
//}