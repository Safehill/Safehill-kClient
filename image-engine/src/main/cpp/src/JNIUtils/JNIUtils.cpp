//
// Created by Zhenxiang Chen on 04/01/24.
//

#include "JNIUtils/JNIUtils.h"

JNIBuffer JNIUtils::getDirectBuffer(JNIEnv *env, jobject &buffer) {
    return {
        .data = static_cast<int8_t *>(env->GetDirectBufferAddress(buffer)),
        .size = static_cast<long>(env->GetDirectBufferCapacity(buffer))
    };
}

std::string JNIUtils::jstringToStdString(JNIEnv *env, jstring jstr) {
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);

    return result;
}
