//
// Created by Zhenxiang Chen on 14/12/23.
//

#ifndef SAFEHILL_MNNHELPER_H
#define SAFEHILL_MNNHELPER_H

#include <MNN/Interpreter.hpp>

class MNNHelper {

public:
    static MNN::Session* createSessionWithBestBackend(MNN::Interpreter *interpreter);
    static MNN::Session* createSessionWithCPUBackend(MNN::Interpreter *interpreter);
};

#endif //SAFEHILL_MNNHELPER_H
