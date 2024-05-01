package com.safehill.kclient.controllers

import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHAssetQuality
import com.safehill.kclient.models.SHEncryptedAsset
import com.safehill.kclient.models.user.SHLocalUserInterface
import com.safehill.kclient.network.ServerProxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.logging.Logger

class LocalAssetStoreController(user: SHLocalUserInterface?, log: Logger?) {
    private val user: SHLocalUserInterface? = null
    private val log: Logger? = null

    private fun serverProxy(): ServerProxy {
        return user.getServerProxy()
    }

    fun globalIdentifiers(): List<String> {
        val identifiersInCache: MutableList<String> = ArrayList()
        val latch = CountDownLatch(1)
        serverProxy().getLocalAssetDescriptors { result ->
            if (result.isSuccess()) {
                identifiersInCache.addAll(
                    result.getDescriptors().stream()
                        .map(SHAssetDescriptor::getGlobalIdentifier)
                        .toList()
                )
            } else {
                log?.severe("failed to get local asset descriptors: " + result.getError().getLocalizedMessage())
            }
            latch.countDown()
        }
        try {
            latch.await(SHDefaultDBTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            log?.severe("the background download operation timed out)
        }
        return identifiersInCache
    }

    fun encryptedAsset(
        globalIdentifier: String,
        versions: List<SHAssetQuality?>?,
        cacheHiResolution: Boolean,
        completionHandler: Consumer<Result<SHEncryptedAsset?>?>
    ) {
        encryptedAssets(java.util.List.of(globalIdentifier), versions, cacheHiResolution, completionHandler)
    }

    fun encryptedAssets(
        globalIdentifiers: List<String>?,
        versions: List<SHAssetQuality?>?,
        cacheHiResolution: Boolean,
        completionHandler: Consumer<Result<Map<String?, SHEncryptedAsset?>?>?>
    ) {
        val assets: MutableMap<String, SHEncryptedAsset> = HashMap()
        var error: Exception? = null
        val latch = CountDownLatch(1)
        serverProxy().getLocalAssets(globalIdentifiers, versions ?: SHAssetQuality.all(), cacheHiResolution) { result ->
            if (result.isSuccess()) {
                assets.putAll(result.getAssets())
            } else {
                error = result.getError()
            }
            latch.countDown()
        }
        latch.await()
        if (error != null) {
            completionHandler.accept(Result.failure(error!!))
        } else {
            completionHandler.accept(Result.success(assets))
        }
    }

    private fun decryptedAsssetInternal(
        encryptedAsset: SHEncryptedAsset,
        quality: SHAssetQuality,
        descriptor: SHAssetDescriptor,
        completionHandler: Consumer<Result<SHDecryptedAsset>>
    ) {
        if (descriptor.getSharingInfo().getSharedByUserIdentifier().equals(user!!.identifier)) {
            try {
                val decryptedAsset: SHDecryptedAsset = user.decrypt(encryptedAsset, quality, user)
                completionHandler.accept(Result.success(decryptedAsset))
            } catch (e: Exception) {
                completionHandler.accept(Result.failure(e))
            }
        } else {
            val usersController = UsersController(user)
            usersController.getUsers(
                java.util.List.of(
                    descriptor.getSharingInfo().getSharedByUserIdentifier()
                )
            ) { result ->
                if (result.isSuccess()) {
                    val usersDict: Map<String, SHServerUser> = result.getUsers()
                    if (usersDict.size == 1) {
                        val serverUser: SHServerUser = usersDict.values.iterator().next()
                        if (serverUser.getIdentifier()
                                .equals(descriptor.getSharingInfo().getSharedByUserIdentifier())
                        ) {
                            try {
                                val decryptedAsset: SHDecryptedAsset = user.decrypt(encryptedAsset, quality, serverUser)
                                completionHandler.accept(Result.success(decryptedAsset))
                            } catch (e: Exception) {
                                completionHandler.accept(Result.failure(e))
                            }
                        } else {
                            completionHandler.accept(
                                Result.failure(
                                    SHBackgroundOperationError(
                                        "unexpectedData",
                                        usersDict
                                    )
                                )
                            )
                        }
                    } else {
                        completionHandler.accept(
                            Result.failure(
                                SHBackgroundOperationError(
                                    "unexpectedData",
                                    usersDict
                                )
                            )
                        )
                    }
                } else {
                    completionHandler.accept(Result.failure(result.getError()))
                }
            }
        }
    }

    fun decryptedAsset(
        encryptedAsset: SHEncryptedAsset,
        quality: SHAssetQuality,
        descriptor: SHAssetDescriptor?,
        completionHandler: Consumer<Result<SHDecryptedAsset>>
    ) {
        if (descriptor != null) {
            decryptedAsssetInternal(encryptedAsset, quality, descriptor, completionHandler)
        } else {
            serverProxy().getLocalAssetDescriptors { result ->
                if (result.isSuccess()) {
                    val optionalDescriptor: Optional<SHAssetDescriptor> = result.getDescriptors().stream()
                        .filter { d -> d.getGlobalIdentifier().equals(encryptedAsset.getGlobalIdentifier()) }
                        .findFirst()
                    if (optionalDescriptor.isPresent()) {
                        val foundDescriptor: SHAssetDescriptor = optionalDescriptor.get()
                        decryptedAsssetInternal(encryptedAsset, quality, foundDescriptor, completionHandler)
                    } else {
                        completionHandler.accept(
                            Result.failure(
                                SHBackgroundOperationError(
                                    "missingAssetInLocalServer",
                                    encryptedAsset.getGlobalIdentifier()
                                )
                            )
                        )
                    }
                } else {
                    completionHandler.accept(Result.failure(result.getError()))
                }
            }
        }
    }

    fun locallyEncryptedVersions(localIdentifier: String?): List<SHAssetQuality> {
        val availableVersions: MutableList<SHAssetQuality> = ArrayList()
        val latch = CountDownLatch(1)
        serverProxy().getLocalAssetDescriptors { result ->
            if (result.isSuccess()) {
                val optionalDescriptor: Optional<SHAssetDescriptor> = result.getDescriptors().stream()
                    .filter { d -> d.getLocalIdentifier().equals(localIdentifier) }
                    .findFirst()
                if (optionalDescriptor.isPresent()) {
                    val descriptor: SHAssetDescriptor = optionalDescriptor.get()
                    val versionsMap: Map<String, List<SHAssetQuality>> =
                        locallyEncryptedVersions(java.util.List.of(descriptor.getGlobalIdentifier()))
                    availableVersions.addAll(versionsMap.values.iterator().next())
                }
            }
            latch.countDown()
        }
        try {
            latch.await(SHDefaultDBTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return availableVersions
    }

    fun locallyEncryptedVersions(globalIdentifiers: List<String?>?): Map<String, List<SHAssetQuality>> {
        val availableVersions: MutableMap<String, List<SHAssetQuality>> = HashMap()
        val latch = CountDownLatch(1)
        serverProxy().getLocalAssets(globalIdentifiers, SHAssetQuality.all(), false) { result ->
            if (result.isSuccess()) {
                for ((key, value) in result.getAssets()
                    .entrySet()) {
                    availableVersions[key] = ArrayList(value.getEncryptedVersions().keySet())
                }
            }
            latch.countDown()
        }
        try {
            latch.await(SHDefaultDBTimeoutInMilliseconds * 2, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return availableVersions
    }

    fun retrieveCommonEncryptionKey(globalIdentifier: String, completionHandler: Consumer<Result<Data?>?>) {
        val encryptionProtocolSalt = user!!.maybeEncryptionProtocolSalt
        if (encryptionProtocolSalt == null) {
            completionHandler.accept(Result.failure(SHLocalUserError("missingProtocolSalt")))
            return
        }
        val quality: SHAssetQuality = SHAssetQuality.lowResolution
        encryptedAsset(globalIdentifier, java.util.List.of<SHAssetQuality?>(quality), false,
            Consumer<Result<SHEncryptedAsset>> { result ->
                if (result.isSuccess()) {
                    val encryptedAsset: SHEncryptedAsset = result.get()
                    val version: SHEncryptedAssetVersion = encryptedAsset.getEncryptedVersions().get(quality)
                    if (version != null) {
                        val encryptedSecret = SHShareablePayload(
                            version.getEphemeralPublicKeyData(),
                            version.getCyphertext(),
                            version.getPublicSignatureData()
                        )
                        try {
                            val userContext = SHUserContext(user.shUser)
                            val encryptionKey: ByteArray = userContext.decryptSecret(
                                encryptedSecret,
                                encryptionProtocolSalt,
                                user.publicSignatureData
                            )
                            completionHandler.accept(Result.success(encryptionKey))
                        } catch (e: Exception) {
                            completionHandler.accept(Result.failure(e))
                        }
                    } else {
                        completionHandler.accept(
                            Result.failure(
                                SHBackgroundOperationError(
                                    "missingAssetInLocalServer",
                                    globalIdentifier
                                )
                            )
                        )
                    }
                } else {
                    completionHandler.accept(Result.failure(result.getError()))
                }
            })
    }
}
