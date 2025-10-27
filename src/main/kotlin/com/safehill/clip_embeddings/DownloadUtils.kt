package com.safehill.clip_embeddings

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareGet
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import java.io.File


suspend fun downloadFileFromUrl(
    url: String,
    destinationFile: File
) {
    val client = HttpClient(CIO) {
        engine {
            requestTimeout = 0
        }
    }

    try {
        // Delete destination file if it exists
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        // Download the file
        client.prepareGet(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.body()
            channel.copyTo(destinationFile.writeChannel())
        }
    } finally {
        client.close()
    }
}
