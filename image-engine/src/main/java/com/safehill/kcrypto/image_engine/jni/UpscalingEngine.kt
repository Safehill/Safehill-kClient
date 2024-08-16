package com.safehill.kcrypto.image_engine.jni

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.nio.ByteBuffer

/**
 * JNI bridge to the native UpscalingEngine implementation that can be used run inference
 * with a pretrained model in MNN format. The model must contain a single RGB input and output
 *
 * @param modelFile the pretrained model to use in .mnn format
 * @param scale expected upscaling factor of the model
 * @param tileSize tile size to use for processing, specify 0 or negative to disable tiling
 */
class UpscalingEngine(val modelFile: File, val scale: Int, val tileSize: Int) {

    private var enginePtr: Long = 0

    /**
     * Create UpscalingEngine instance from [File]
     * @return native pointer to the instance
     */
    private external fun createUpscalingEngineFile(
        errorValue: ByteBuffer,
        modelAbsolutePath: String,
        scale: Int,
        tileSize: Int,
        placeholderColour: Int,
    ): Long

    /**
     * Destroy an UpscalingEngine instance
     * @param ptr native pointer to the instance
     */
    private external fun destroyUpscalingEngine(ptr: Long)

    private external fun runUpscaling(
        upscalingEnginePtr: Long,
        progressTracker: JNIProgressTracker,
        coroutineScope: CoroutineScope,
        inputBitmap: Bitmap,
        outputBitmap: Bitmap
    ): Int

    /**
     * Run inference the [inputBitmap] and [outputBitmap] must use [Bitmap.Config.ARGB_8888]
     */
    fun runUpscaling(
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
            enginePtr = createUpscalingEngineFile(
                errorValue = errorValue,
                modelAbsolutePath = modelFile.absolutePath,
                scale = scale,
                tileSize = tileSize,
                placeholderColour = placeholderColour
            )
            val errorCode = errorValue.get().toInt()
            // Failed to init engine, return error code
            if (errorCode != 0) {
                return MNNInterpreterError.Companion.fromNativeErrorEnum(
                    errorCode
                )
            }
        }

        val errorCode = runUpscaling(
            upscalingEnginePtr = enginePtr,
            progressTracker = progressTracker,
            coroutineScope = coroutineScope,
            inputBitmap = inputBitmap,
            outputBitmap = outputBitmap
        )
        // Upscaling failed, return error code
        if (errorCode != 0) return MNNInterpreterError.Companion.fromNativeErrorEnum(
            errorCode
        )

        return null
    }

    fun freeResources() {
        if (enginePtr != 0L) {
            destroyUpscalingEngine(enginePtr)
            enginePtr = 0
        }
    }

    companion object {
        // Used to load the 'realesrgan' library on application startup.
        init {
            // System.loadLibrary("MNN_Vulkan")
            // System.loadLibrary("MNN_CL")
            System.loadLibrary("UpscalingEngine")
        }
    }
}