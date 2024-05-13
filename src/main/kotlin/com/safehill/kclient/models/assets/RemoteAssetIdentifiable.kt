package com.safehill.kclient.models.assets

interface RemoteAssetIdentifiable {
    val globalIdentifier: String
    val localIdentifier: String?
}