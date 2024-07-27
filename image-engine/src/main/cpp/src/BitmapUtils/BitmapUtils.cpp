//
// Created by Zhenxiang Chen on 16/12/23.
//

#include "BitmapUtils/BitmapUtils.h"

PixelMatrix BitmapUtils::pixelMatrixFromBitmap(JNIEnv *env, jobject bitmap) {
    jclass clazz = env->GetObjectClass(bitmap);
    const image_dimensions bitmapDimens = {
            .width = env->CallIntMethod(bitmap, env->GetMethodID(clazz, "getWidth", "()I")),
            .height = env->CallIntMethod(bitmap, env->GetMethodID(clazz, "getHeight", "()I"))
    };
    void* bitmapBuffer;
    AndroidBitmap_lockPixels(env, bitmap, &bitmapBuffer);

    return PixelMatrix(static_cast<int*>(bitmapBuffer),bitmapDimens.height,bitmapDimens.width);
}
