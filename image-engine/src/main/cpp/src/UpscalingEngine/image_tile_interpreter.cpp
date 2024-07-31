//
// Created by Zhenxiang Chen on 06/02/23.
//

#include <thread>

#include <MNNHelper/MNNHelper.h>
#include <UpscalingEngine/image_tile_interpreter.h>

void ImageTileInterpreter::inference() const {
    // Feed data to the interpreter
    interpreter_input->copyFromHostTensor(input_tensor);

    // Run the interpreter
    interpreter->runSession(session);

    // Extract result from interpreter
    interpreter_output->copyToHostTensor(output_tensor);
}

ImageTileInterpreter::~ImageTileInterpreter() {
    releaseSession();
    MNN::Interpreter::destroy(interpreter);
}

void ImageTileInterpreter::updateTileSize(const image_dimensions* tile_size) {
    // Reconfigure interpreter on tile size change
    if (
            interpreter_input == nullptr ||
            interpreter_input->height() != tile_size->height ||
            interpreter_input->width() != tile_size->width) {

        releaseSession();

        session = MNNHelper::createSessionWithCPUBackend(interpreter);
        if (session == nullptr) {
            throw ImageTileInterpreterException(ImageTileInterpreterException::Error::CreateBackendFailed);
        }
        interpreter_input = interpreter->getSessionInput(session, nullptr);
        // We store matrix as row major so ignore MNN default tensor orientation
        interpreter->resizeTensor(
                interpreter_input,
                1,
                REALESRGAN_IMAGE_CHANNELS,
                tile_size->height,
                tile_size->width);
        interpreter->resizeSession(session, 0);
        interpreter_output = interpreter->getSessionOutput(session, nullptr);

        input_tensor = new MNN::Tensor(interpreter_input, MNN::Tensor::CAFFE);
        output_tensor = new MNN::Tensor(interpreter_output, MNN::Tensor::CAFFE);

        input_buffer = input_tensor->host<float>();
        output_buffer = output_tensor->host<float>();
    }
}

void ImageTileInterpreter::releaseSession() {
    MNN::Tensor::destroy(input_tensor);
    MNN::Tensor::destroy(output_tensor);
    if (session != nullptr) {
        interpreter->releaseSession(session);
    }
}

ImageTileInterpreter::ImageTileInterpreter(const char *model_path) {
    interpreter = MNN::Interpreter::createFromFile(model_path);
    if (interpreter == nullptr) {
        throw ImageTileInterpreterException(ImageTileInterpreterException::Error::CreateInterpreterFailed);
    }
}
