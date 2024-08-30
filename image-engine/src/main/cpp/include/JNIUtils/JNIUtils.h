//
// Created by Zhenxiang Chen on 04/01/24.
//

#ifndef SAFEHILL_JNIUTILS_H
#define SAFEHILL_JNIUTILS_H

#include <string>
#include <jni.h>

struct JNIBuffer {
    int8_t* data;
    long size;
};

class JNIUtils {

public:
    static JNIBuffer getDirectBuffer(JNIEnv* env, jobject &buffer);
    static std::string jstringToStdString(JNIEnv* env, jstring jstr);
};

#endif //SAFEHILL_JNIUTILS_H
