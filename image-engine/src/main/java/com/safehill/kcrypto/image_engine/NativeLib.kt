package com.safehill.kcrypto.image_engine

class NativeLib {

    /**
     * A native method that is implemented by the 'image_engine' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'image_engine' library on application startup.
        init {
            System.loadLibrary("image_engine")
        }
    }
}