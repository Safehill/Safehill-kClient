package com.safehill.kclient.utils

interface ImageResizerInterface {
    fun resizeImageIfLarger(imageBytes: ByteArray, width: Int, height: Int): ByteArray

}
