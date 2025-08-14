package com.safehill.clip_embeddings

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@OptIn(ExperimentalStdlibApi::class)
fun File.sha256(): String {
    return FileInputStream(this).use { fis ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        digest.digest().toHexString()
    }
}


fun InputStream.unZippedStreamFirstEntry(): ZipInputStream {
    val zipInputStream = ZipInputStream(BufferedInputStream(this))
    val firstEntry: ZipEntry? = zipInputStream.nextEntry
    if (firstEntry == null || firstEntry.isDirectory) {
        zipInputStream.close()
        throw IllegalStateException("Zip archive does not contain a valid file entry")
    }
    return zipInputStream
}