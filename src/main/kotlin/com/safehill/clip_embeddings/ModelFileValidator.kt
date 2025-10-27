package com.safehill.clip_embeddings

import com.safehill.kclient.util.runCatchingSafe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import java.io.File

class ModelFileValidator(
    private val downloadDirectory: DownloadDirectory
) {

    suspend fun isFileValid(file: File): Boolean {
        val file = downloadDirectory.getOnnxFile
        if (!file.exists() || file.length() == 0L) {
            return false
        }
        val expectedHash = getExpectedHash()

        if (expectedHash == null) {
            return false
        }
        val hash = file.sha256()
        val isValid = hash.trim() == expectedHash.trim()
        return isValid
    }


    private suspend fun getExpectedHash(): String? {
        val hashFile = File(downloadDirectory.file, "TinyCLIP.onnx.sha256")
        val client = HttpClient(CIO)
        return try {
            runCatchingSafe {
                val hashFromServer = client.get(CLIP_HASH_URL).body<String>()
                hashFile.writeBytes(hashFromServer.toByteArray())
                hashFromServer
            }.getOrNull() ?: String(hashFile.readBytes())
        } catch (e: Throwable) {
            null
        } finally {
            client.close()
        }
    }
}

@JvmInline
value class DownloadDirectory(
    val file: File
) {
    val getOnnxFile
        get() = File(this.file, "TinyCLIP.onnx")
}


