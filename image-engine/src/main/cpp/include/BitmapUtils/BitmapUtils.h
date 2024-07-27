//
// Created by Zhenxiang Chen on 16/12/23.
//

#ifndef SUPERIMAGE_BITMAPUTILS_H
#define SUPERIMAGE_BITMAPUTILS_H

#include "ImageUtils/Types.h"

#include <android/bitmap.h>
#include <jni.h>
#include <opencv2/opencv.hpp>

class BitmapUtils {

public:
    static PixelMatrix pixelMatrixFromBitmap(JNIEnv* env, jobject bitmap);
};

#endif //SUPERIMAGE_BITMAPUTILS_H
