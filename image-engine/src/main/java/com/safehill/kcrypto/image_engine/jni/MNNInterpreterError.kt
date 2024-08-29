package com.safehill.kcrypto.image_engine.jni

sealed interface MNNInterpreterError {

    object CreateInterpreter: MNNInterpreterError

    object CreateBackend: MNNInterpreterError

    companion object {

        /**
         * Convert from ImageTileInterpreterException::Error to [Error]
         */
        internal fun fromNativeErrorEnum(value: Int) = when(value) {
            1 -> CreateInterpreter
            2 -> CreateBackend
            else -> throw IllegalStateException("Unmapped interpreter error $value")
        }
    }
}