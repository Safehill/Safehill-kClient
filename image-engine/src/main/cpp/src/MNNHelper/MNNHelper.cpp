//
// Created by Zhenxiang Chen on 14/12/23.
//

#include <MNNHelper/MNNHelper.h>
#include <thread>

MNN::Session* createSessionAutoBackend(MNN::Interpreter* interpreter, MNN::ScheduleConfig& config) {
#ifdef __APPLE__
    const MNNForwardType backends[] = { MNN_FORWARD_METAL, MNN_FORWARD_OPENCL };
#elif _WIN32
    const MNNForwardType backends[] = { MNN_FORWARD_CUDA, MNN_FORWARD_VULKAN, MNN_FORWARD_OPENCL };
#else
    const MNNForwardType backends[] = { MNN_FORWARD_VULKAN, MNN_FORWARD_OPENCL };
#endif
    MNN::Session* session;
    const int backends_count = std::size(backends);
    const bool is_odd = backends_count % 2 != 0;
    // Test the backends we want in pairs
    for (int i = 0; i < backends_count - is_odd; i += 2) {
        config.type = backends[i];
        config.backupType = backends[i + 1];
        session = interpreter->createSession(config);
        if (session != nullptr) {
            return session;
        }
    }
    if (is_odd) {
        config.type = backends[backends_count - 1];
        config.backupType = backends[backends_count - 1];
        session = interpreter->createSession(config);
    }
    return session;
}

MNN::Session* MNNHelper::createSessionWithBestBackend(MNN::Interpreter *interpreter) {
    MNN::ScheduleConfig config;
    MNN::BackendConfig backendConfig;
    backendConfig.memory = MNN::BackendConfig::Memory_High;
    backendConfig.power = MNN::BackendConfig::Power_High;
    backendConfig.precision = MNN::BackendConfig::Precision_Low;
    config.backendConfig = &backendConfig;
    config.numThread = std::thread::hardware_concurrency();
    return createSessionAutoBackend(interpreter, config);
}

MNN::Session* MNNHelper::createSessionWithCPUBackend(MNN::Interpreter *interpreter) {
    MNN::ScheduleConfig config;
    MNN::BackendConfig backendConfig;
    backendConfig.memory = MNN::BackendConfig::Memory_High;
    backendConfig.power = MNN::BackendConfig::Power_High;
    backendConfig.precision = MNN::BackendConfig::Precision_Low;
    config.backendConfig = &backendConfig;
    config.numThread = std::thread::hardware_concurrency();
    config.type = MNN_FORWARD_CPU;
    config.backupType = MNN_FORWARD_CPU;
    return interpreter->createSession(config);
}
