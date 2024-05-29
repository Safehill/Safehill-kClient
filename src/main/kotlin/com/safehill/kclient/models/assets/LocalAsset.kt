package com.safehill.kclient.models.assets

import java.util.Date

typealias AssetLocalIdentifier = String
typealias AssetGlobalIdentifier = String

class LocalAsset(
    val localIdentifier: AssetLocalIdentifier,
    val createdAt: Date?,
    val data: ByteArray
)
