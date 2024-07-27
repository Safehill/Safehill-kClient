//
// Created by Zhenxiang Chen on 29/06/24.
//

#include <jni.h>

#include "JNIUtils/JNIUtils.h"
#include "UpscalingEngine/UpscalingEngine.h"
#ifdef __ANDROID__
#include "BitmapUtils/BitmapUtils.h"
#include "BitmapUtils/AndroidBitmapWrapper.h"
#else
#include "ImageUtils/ImageUtils.h"
#include "coroutine_utils.h"
#endif

#ifdef __ANDROID__
extern "C" JNIEXPORT jint JNICALL
Java_com_safehill_kcrypto_image_1engine_UpscalingEngine_runUpscaling(
        JNIEnv *env,
        jobject /* thiz */,
        jlong upscaling_engine_ptr,
        jobject progress_tracker,
        jobject coroutine_scope,
        jobject input_bitmap,
        jobject output_bitmap) {

    auto upscalingEngine = reinterpret_cast<UpscalingEngine *>(upscaling_engine_ptr);

    auto inputImageWrapper = AndroidBitmapWrapper(env, input_bitmap);
    auto outputImageWrapper = AndroidBitmapWrapper(env, output_bitmap);
    jint error = 0;

    try {
        upscalingEngine->upscaleImage(
                env,
                progress_tracker,
                coroutine_scope,
                inputImageWrapper.getCVMat(),
                outputImageWrapper.getCVMat());
    } catch (ImageTileInterpreterException& e) {
        error = e.error;
    }

    return error;
}
#else
extern "C" JNIEXPORT jint JNICALL
Java_com_safehill_kcrypto_image_1engine_UpscalingEngine_runUpscaling(
        JNIEnv *env,
        jobject /* thiz */,
        jlong upscaling_engine_ptr,
        jobject progress_tracker,
        jobject coroutine_scope,
        jstring input_image_absolute_path,
        jstring output_image_absolute_path,
        jint output_format_id) {

    auto upscalingEngine = reinterpret_cast<UpscalingEngine *>(upscaling_engine_ptr);
    const char* inputImageAbsolutePathStr = env->GetStringUTFChars(input_image_absolute_path, nullptr);
    const char* outputImageAbsolutePathStr = env->GetStringUTFChars(output_image_absolute_path, nullptr);

    cv::Mat input_image_matrix = ImageUtils::loadImageAsRGBAMat(inputImageAbsolutePathStr);
    cv::Mat output_image_mat = cv::Mat(input_image_matrix.rows * upscalingEngine->scale,
                                       input_image_matrix.cols * upscalingEngine->scale,
                                       input_image_matrix.type());

    jint error = 0;

    try {
        upscalingEngine->upscaleImage(
                env,
                progress_tracker,
                coroutine_scope,
                input_image_matrix,
                output_image_mat);
    } catch (ImageTileInterpreterException& e) {
        error = e.error;
    }

    if (is_coroutine_scope_active(env, coroutine_scope)) {
        ImageUtils::writeImageRGBAMat(
            outputImageAbsolutePathStr,
            output_image_mat,
            static_cast<ImageUtils::OutputFormat>(output_format_id));
    }

    // Cleanup
    env->ReleaseStringUTFChars(input_image_absolute_path, inputImageAbsolutePathStr);
    env->ReleaseStringUTFChars(output_image_absolute_path, outputImageAbsolutePathStr);

    return error;
}
#endif

extern "C"
JNIEXPORT void JNICALL
Java_com_safehill_kcrypto_image_1engine_UpscalingEngine_destroyUpscalingEngine(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong upscaling_engine_ptr) {
    auto upscalingEnginePtr = reinterpret_cast<UpscalingEngine *>(upscaling_engine_ptr);

    delete upscalingEnginePtr;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_safehill_kcrypto_image_1engine_UpscalingEngine_createUpscalingEngineFile(JNIEnv *env,
                                                                        jobject/* thiz */,
                                                                        jobject error_value_buffer,
                                                                        jstring model_absolute_path,
                                                                        jint scale,
                                                                        jint tile_size,
                                                                        jint placeholder_colour) {

    const char* filePathStr = env->GetStringUTFChars(model_absolute_path, nullptr);
    const auto error_value = reinterpret_cast<int8_t*>(JNIUtils::getDirectBuffer(env, error_value_buffer).data);
    jlong ptr;
    try {
        auto upscalingEngine = new UpscalingEngine(filePathStr, scale, tile_size, placeholder_colour);
        error_value[0] = 0;
        ptr = (jlong) upscalingEngine;
    } catch (ImageTileInterpreterException& e) {
        error_value[0] = e.error;
        ptr = 0;
    }
    env->ReleaseStringUTFChars(model_absolute_path, filePathStr);
    return ptr;
}
