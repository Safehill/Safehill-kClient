//
// Created by Zhenxiang Chen on 30/06/24.
//

#include "BitmapUtils/AndroidBitmapWrapper.h"
#include <android/bitmap.h>
#include <stdexcept>

AndroidBitmapWrapper::AndroidBitmapWrapper(JNIEnv *env, jobject bitmap): env(env), bitmap(bitmap) {
    void* pixelsPtr;
    AndroidBitmapInfo info;

    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        throw std::runtime_error("Failed to get bitmap info");
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throw std::runtime_error("Bitmap format is not RGBA_8888");
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixelsPtr) < 0) {
        throw std::runtime_error("Failed to lock bitmap pixels");
    }

    mat = cv::Mat(info.height, info.width, CV_8UC4, pixelsPtr);
}

AndroidBitmapWrapper::~AndroidBitmapWrapper() {
    AndroidBitmap_unlockPixels(env, bitmap);
}

cv::Mat &AndroidBitmapWrapper::getCVMat() {
    return mat;
}
