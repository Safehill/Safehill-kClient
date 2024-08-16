package com.safehill.kclient.models.assets

import java.time.Instant

typealias AssetLocalIdentifier = String
typealias AssetGlobalIdentifier = String

class LocalAsset(
    val localIdentifier: AssetLocalIdentifier,
    val createdAt: Instant?,
    val data: ByteArray
)
