package com.safehill.kclient.models.assets

interface RemoteAssetIdentifiable {
    val globalIdentifier: AssetGlobalIdentifier
    val localIdentifier: AssetLocalIdentifier?
}