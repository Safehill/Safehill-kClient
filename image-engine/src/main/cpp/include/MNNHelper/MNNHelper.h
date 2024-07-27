//
// Created by Zhenxiang Chen on 14/12/23.
//

#ifndef SUPERIMAGE_MNNHELPER_H
#define SUPERIMAGE_MNNHELPER_H

#include <MNN/Interpreter.hpp>

class MNNHelper {

public:
    static MNN::Session* createSessionWithBestBackend(MNN::Interpreter *interpreter);
    static MNN::Session* createSessionWithCPUBackend(MNN::Interpreter *interpreter);
};

#endif //SUPERIMAGE_MNNHELPER_H
