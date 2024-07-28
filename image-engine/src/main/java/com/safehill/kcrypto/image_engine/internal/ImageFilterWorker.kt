package com.safehill.kcrypto.image_engine.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import com.safehill.kcrypto.image_engine.model.ImageFilterConfig
import com.safehill.kcrypto.image_engine.model.MLModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

internal class ImageFilterWorker(
    appContext: Context,
    params: WorkerParameters
): CoroutineWorker(appContext, params) {

    private val config = ImageFilterConfigUtils.fromWorkData(params.inputData)
    private val mlModelsRepository = MLModelsRepository(appContext)
    private val notificationManager = NotificationManagerCompat.from(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        markAsForeground()

        val inputBitmap = readInputBitmap()
        lateinit var result: DownloadModelState

        mlModelsRepository.getOrDownloadModel(config.type.mlModel).collect {
            result = it
        }

        val modelFile = when (val it = result) {
            DownloadModelState.Error -> return@withContext Result.failure()
            is DownloadModelState.Loading -> throw IllegalStateException("")
            is DownloadModelState.Success -> it.file
        }
        val upscalingEngine = UpscalingEngine(modelFile, 1, 0)
        val jniProgressTracker = JNIProgressTracker()

        // Resume inputBitmap to save memory since both input and output have the same resolution
        upscalingEngine.runUpscaling(
            progressTracker = jniProgressTracker,
            coroutineScope = this,
            inputBitmap = inputBitmap,
            outputBitmap = inputBitmap,
            placeholderColour = Color.WHITE
        )

        applicationContext.cacheDir.resolve("${UUID.randomUUID()}.png").apply {
            createNewFile()
        }.outputStream().use {
            inputBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        Result.success()
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

    private fun readInputBitmap() = applicationContext.contentResolver
        .openInputStream(config.inputImage)
        .use(BitmapFactory::decodeStream)

    private companion object {

        val ImageFilterConfig.Type.mlModel get() = when (this) {
            ImageFilterConfig.Type.Cuphead -> TODO()
            ImageFilterConfig.Type.Mosaic -> TODO()
            ImageFilterConfig.Type.StarryNight -> MLModel.StarryNight
        }

        const val IMAGE_PROCESSING_NOTIFICATION_CHANNEL_ID = "image_processing"
    }
}