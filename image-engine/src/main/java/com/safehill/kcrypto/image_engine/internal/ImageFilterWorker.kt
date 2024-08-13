package com.safehill.kcrypto.image_engine.internal

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.safehill.kcrypto.image_engine.MLModelsRepository
import com.safehill.kcrypto.image_engine.R
import com.safehill.kcrypto.image_engine.jni.JNIProgressTracker
import com.safehill.kcrypto.image_engine.jni.UpscalingEngine
import com.safehill.kcrypto.image_engine.model.DownloadModelState
import com.safehill.kcrypto.image_engine.model.ImageFilterArgs
import com.safehill.kcrypto.image_engine.model.ImageFilterWorkState
import com.safehill.kcrypto.image_engine.model.MLModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

internal class ImageFilterWorker(
    appContext: Context,
    params: WorkerParameters
): CoroutineWorker(appContext, params) {

    private val config = ImageFilterConfigUtils.fromWorkData(params.inputData)
    private val mlModelsRepository = MLModelsRepository(appContext)
    private val notificationManager = NotificationManagerCompat.from(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Avoid re-launching work after crash
        if (runAttemptCount > 0) {
            return@withContext Result.failure()
        }

        markAsForeground()

        val inputBitmap = readInputBitmap() ?: return@withContext Result.failure()
        val modelFile = when (val it = downloadMLModel()) {
            DownloadModelState.Error -> return@withContext Result.failure()
            is DownloadModelState.Loading -> throw IllegalStateException("Download model ended with $it")
            is DownloadModelState.Success -> it.file
        }
        val upscalingEngine = UpscalingEngine(modelFile, 1, 0)

        try {
            val jniProgressTracker = JNIProgressTracker()
            val inferenceProgressJob = launch { emitProgressFromTracker(jniProgressTracker) }
            val startTime = SystemClock.elapsedRealtime()

            // Reuse inputBitmap to save memory since both input and output have the same resolution
            upscalingEngine.runUpscaling(
                progressTracker = jniProgressTracker,
                coroutineScope = this,
                inputBitmap = inputBitmap,
                outputBitmap = inputBitmap,
                placeholderColour = Color.WHITE
            )
            println("Inference time = ${SystemClock.elapsedRealtime() - startTime}ms")

            inferenceProgressJob.cancelAndJoin()

            saveOutputBitmap(inputBitmap)?.let { Result.success() } ?: Result.failure()
        } finally {
            upscalingEngine.freeResources()
        }
    }

    private suspend fun downloadMLModel(): DownloadModelState {
        lateinit var result: DownloadModelState

        mlModelsRepository.getOrDownloadModel(config.type.mlModel).collect {
            result = it

            if (it is DownloadModelState.Loading) {
                val progress = if (it.totalBytes == 0L) {
                    ImageFilterWorkState.Running.INDETERMINATE_PROGRESS_VALUE
                } else {
                    it.downloadBytes / it.totalBytes.toFloat()
                }

                setProgress(
                    ImageFilterWorkStateUtils.toProgressData(
                        ImageFilterWorkState.Running(
                            progress,
                            ImageFilterWorkState.Running.Step.DownloadResources
                        )
                    )
                )
            }
        }

        return result
    }

    private suspend fun emitProgressFromTracker(jniProgressTracker: JNIProgressTracker) {
        setProgress(
            ImageFilterWorkStateUtils.toProgressData(
                ImageFilterWorkState.Running(
                    ImageFilterWorkState.Running.INDETERMINATE_PROGRESS_VALUE,
                    ImageFilterWorkState.Running.Step.ProcessImage
                )
            )
        )

        jniProgressTracker.progressFlow.collect {
            val progress = if (it.value == JNIProgressTracker.INDETERMINATE_PROGRESS) {
                ImageFilterWorkState.Running.INDETERMINATE_PROGRESS_VALUE
            } else {
                it.value / 100
            }

            setProgress(
                ImageFilterWorkStateUtils.toProgressData(
                    ImageFilterWorkState.Running(
                        progress,
                        ImageFilterWorkState.Running.Step.ProcessImage
                    )
                )
            )
        }
    }

    // TODO: implement write output bitmap on pre-Q devices (since they don't have scoped storage)
    private fun saveOutputBitmap(bitmap: Bitmap): Uri? {
        val targetUri = applicationContext.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Images.ImageColumns.DISPLAY_NAME, "${UUID.randomUUID()}.png")
                put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}${File.separatorChar}Snoog"
                )
            }
        ) ?: return null

        val writeSuccess = applicationContext.contentResolver.openOutputStream(targetUri)?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        } ?: false

        return targetUri.takeIf { writeSuccess }
    }

    private fun registerNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                IMAGE_PROCESSING_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName("Image processing")
                .build()
        )
    }

    private suspend fun markAsForeground() {
        registerNotificationChannel()

        val notification = NotificationCompat.Builder(
            applicationContext,
            IMAGE_PROCESSING_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("Image processing")
            .setTicker("Image processing")
            .setSmallIcon(R.drawable.baseline_auto_awesome_24)
            .setOngoing(true)
            .build()
        setForeground(ForegroundInfo(0, notification))
    }

    private fun readInputBitmap(): Bitmap? = applicationContext.contentResolver
        .openInputStream(config.inputImage)
        .use(BitmapFactory::decodeStream)

    private companion object {

        val ImageFilterArgs.Type.mlModel get() = when (this) {
            ImageFilterArgs.Type.Cuphead -> MLModel.Cuphead
            ImageFilterArgs.Type.Mosaic -> MLModel.Mosaic
            ImageFilterArgs.Type.StarryNight -> MLModel.StarryNight
        }

        const val IMAGE_PROCESSING_NOTIFICATION_CHANNEL_ID = "image_processing"
    }
}