package com.safehill.kclient.network.local

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.SymmetricKey

interface EncryptionHelper {
    suspend fun getEncryptionKey(globalIdentifier: AssetGlobalIdentifier): SymmetricKey?
    suspend fun saveEncryptionKey(globalIdentifier: AssetGlobalIdentifier, symmetricKey: SymmetricKey)
}
