//
// Created by Zhenxiang Chen on 06/02/23.
//

#ifndef SAFEHILL_IMAGE_TILE_INTERPRETER_H
#define SAFEHILL_IMAGE_TILE_INTERPRETER_H

#include <MNN/Interpreter.hpp>

#include "ImageUtils/Types.h"

#define REALESRGAN_IMAGE_CHANNELS 3


class ImageTileInterpreterException : public std::exception {

public:

    enum Error {
        CreateInterpreterFailed = 1,
        CreateBackendFailed = 2
    };

    const Error error;

    ImageTileInterpreterException(Error error) : error(error) {}

    const char* what() const noexcept override {
        switch (error) {
            case Error::CreateInterpreterFailed:
                return "Failed to create MNN interpreter";
            case Error::CreateBackendFailed:
                return "Failed to create MNN backend";
        }
    }
};

class ImageTileInterpreter {

public:
    ImageTileInterpreter(const char* model_path);
    ~ImageTileInterpreter();

    MNN::Tensor* input_tensor = nullptr;
    MNN::Tensor* output_tensor = nullptr;

    void inference() const;

    void updateTileSize(const image_dimensions* tile_size);

private:
    MNN::Interpreter* interpreter = nullptr;
    MNN::Session* session = nullptr;
    MNN::Tensor* interpreter_input = nullptr;
    MNN::Tensor* interpreter_output = nullptr;

    void releaseSession();
};

#endif //SAFEHILL_IMAGE_TILE_INTERPRETER_H
