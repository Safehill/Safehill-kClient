//
// Created by Zhenxiang Chen on 30/06/24.
//

#ifndef SUPERIMAGE_ANDROIDBITMAPWRAPPER_H
#define SUPERIMAGE_ANDROIDBITMAPWRAPPER_H

#include <jni.h>
#include <opencv2/opencv.hpp>

class AndroidBitmapWrapper {
public:
    AndroidBitmapWrapper(JNIEnv *env, jobject bitmap);
    ~AndroidBitmapWrapper();

    cv::Mat& getCVMat();
private:
    JNIEnv *env;
    jobject bitmap;
    cv::Mat mat;
};
#endif //SUPERIMAGE_ANDROIDBITMAPWRAPPER_H
