package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.SymmetricKey

interface AssetEncrypterInterface {
    suspend fun encryptedAsset(
        outboundQueueItem: OutboundQueueItem,
        user: LocalUser
    ): EncryptedAsset

}
