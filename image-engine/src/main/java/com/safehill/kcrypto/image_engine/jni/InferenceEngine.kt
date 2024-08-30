package com.safehill.kcrypto.image_engine.jni

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.nio.ByteBuffer

/**
 * JNI bridge to the native InferenceEngine implementation that can be used run inference
 * with a pretrained model in MNN format. The model must contain a single RGB input and output
 *
 * @param modelFile the pretrained model to use in .mnn format
 * @param scale expected upscaling factor of the model
 * @param tileSize tile size to use for processing, specify 0 or negative to disable tiling
 */
class InferenceEngine(val modelFile: File, val scale: Int, val tileSize: Int) {

    private var enginePtr: Long = 0

    /**
     * Create InferenceEngine instance from [File]
     * @return native pointer to the instance
     */
    private external fun createEngineFile(
        errorValue: ByteBuffer,
        modelAbsolutePath: String,
        scale: Int,
        tileSize: Int,
        placeholderColour: Int,
    ): Long

    /**
     * Destroy an InferenceEngine instance
     * @param ptr native pointer to the instance
     */
    private external fun destroyEngine(ptr: Long)

    private external fun runInference(
        enginePtr: Long,
        progressTracker: JNIProgressTracker,
        coroutineScope: CoroutineScope,
        inputBitmap: Bitmap,
        outputBitmap: Bitmap
    ): Int

    /**
     * Run inference the [inputBitmap] and [outputBitmap] must use [Bitmap.Config.ARGB_8888]
     */
    fun runInference(
        progressTracker: JNIProgressTracker,
        coroutineScope: CoroutineScope,
        inputBitmap: Bitmap,
        outputBitmap: Bitmap,
        placeholderColour: Int,
    ): MNNInterpreterError? {
        require(inputBitmap.config == Bitmap.Config.ARGB_8888) {
            "Input bitmap must have ARGB_8888 config"
        }
        require(outputBitmap.config == Bitmap.Config.ARGB_8888) {
            "Output bitmap must have ARGB_8888 config"
        }

        if (enginePtr == 0L) {
            val errorValue = ByteBuffer.allocateDirect(1)
            enginePtr = createEngineFile(
                errorValue = errorValue,
                modelAbsolutePath = modelFile.absolutePath,
                scale = scale,
                tileSize = tileSize,
                placeholderColour = placeholderColour
            )
            val errorCode = errorValue.get().toInt()
            // Failed to init engine, return error code
            if (errorCode != 0) {
                return MNNInterpreterError.fromNativeErrorEnum(
                    errorCode
                )
            }
        }

        val errorCode = runInference(
            enginePtr = enginePtr,
            progressTracker = progressTracker,
            coroutineScope = coroutineScope,
            inputBitmap = inputBitmap,
            outputBitmap = outputBitmap
        )
        // Inference failed, return error code
        if (errorCode != 0) return MNNInterpreterError.fromNativeErrorEnum(
            errorCode
        )

        return null
    }

    fun freeResources() {
        if (enginePtr != 0L) {
            destroyEngine(enginePtr)
            enginePtr = 0
        }
    }

    companion object {
        // Used to load the native libraries on application startup.
        init {
            // System.loadLibrary("MNN_Vulkan")
            // System.loadLibrary("MNN_CL")
            System.loadLibrary("InferenceEngine")
        }
    }
}