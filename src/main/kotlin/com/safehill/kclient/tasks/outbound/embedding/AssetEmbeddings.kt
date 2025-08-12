package com.safehill.kclient.tasks.outbound.embedding

interface AssetEmbeddings {
    suspend fun getEmbeddings(data: ByteArray): FloatArray
}