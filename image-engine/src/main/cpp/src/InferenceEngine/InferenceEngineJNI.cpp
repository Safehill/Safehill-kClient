//
// Created by Zhenxiang Chen on 29/06/24.
//

#include <jni.h>

#include "JNIUtils/JNIUtils.h"
#include "InferenceEngine/InferenceEngine.h"
#ifdef __ANDROID__
#include "BitmapUtils/AndroidBitmapWrapper.h"
#else
#include "ImageUtils/ImageUtils.h"
#include "coroutine_utils.h"
#endif

extern "C" JNIEXPORT jint JNICALL
Java_com_safehill_kcrypto_image_1engine_jni_InferenceEngine_runInference(
        JNIEnv *env,
        jobject /* thiz */,
        jlong inference_engine_ptr,
        jobject progress_tracker,
        jobject coroutine_scope,
        jobject input_bitmap,
        jobject output_bitmap) {

    auto inferenceEngine = reinterpret_cast<InferenceEngine *>(inference_engine_ptr);

    auto inputImageWrapper = AndroidBitmapWrapper(env, input_bitmap);
    auto outputImageWrapper = AndroidBitmapWrapper(env, output_bitmap);
    jint error = 0;

    try {
        inferenceEngine->runInference(
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

extern "C"
JNIEXPORT void JNICALL
Java_com_safehill_kcrypto_image_1engine_jni_InferenceEngine_destroyEngine(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong inference_engine_ptr) {
    auto inferenceEnginePtr = reinterpret_cast<InferenceEngine *>(inference_engine_ptr);

    delete inferenceEnginePtr;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_safehill_kcrypto_image_1engine_jni_InferenceEngine_createEngineFile(JNIEnv *env,
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
        auto inferenceEngine = new InferenceEngine(filePathStr, scale, tile_size, placeholder_colour);
        error_value[0] = 0;
        ptr = (jlong) inferenceEngine;
    } catch (ImageTileInterpreterException& e) {
        error_value[0] = e.error;
        ptr = 0;
    }
    env->ReleaseStringUTFChars(model_absolute_path, filePathStr);
    return ptr;
}
