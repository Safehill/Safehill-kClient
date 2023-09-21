package com.safehill.kclient.models

interface SHRemoteAssetIdentifiable {
    val globalIdentifier: String
    val localIdentifier: String?
}