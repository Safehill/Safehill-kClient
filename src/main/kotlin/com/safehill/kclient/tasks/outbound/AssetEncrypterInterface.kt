package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.ShareablePayload
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser

interface AssetEncrypterInterface {
    suspend fun encryptedAsset(
        outboundQueueItem: OutboundQueueItem,
        user: LocalUser,
        recipient: ServerUser = user
    ): EncryptedAsset

    suspend fun getSharablePayload(
        outboundQueueItem: OutboundQueueItem,
        user: LocalUser,
        recipient: ServerUser
    ): Pair<SymmetricKey, ShareablePayload>
}
