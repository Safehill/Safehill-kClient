//
// Created by Zhenxiang Chen on 24/08/23.
//

#ifndef SUPERIMAGE_UPSCALINGENGINE_H
#define SUPERIMAGE_UPSCALINGENGINE_H

#include <jni.h>
#include <opencv2/opencv.hpp>
#include "unsupported/Eigen/CXX11/Tensor"

#include "ImageUtils/Types.h"
#include "image_tile_interpreter.h"

#define REALESRGAN_INPUT_TILE_PADDING 10

class UpscalingEngine {

public:
    UpscalingEngine(const char* model_path,
                    const int scale,
                    const uint32_t tile_size,
                    const int32_t placeholderColour);
    ~UpscalingEngine();

    const int scale;

    const int tileSize;

    /**
     * Colour that will replace a transparent pixel since Real-ESRGAN doesn't support upscaling
     */
    const int32_t placeholderColour;

    void upscaleImage(
            JNIEnv* jni_env,
            jobject progress_tracker,
            jobject coroutine_scope,
            cv::Mat &input_image_matrix,
            cv::Mat &output_image_matrix);

private:
    ImageTileInterpreter interpreter;

    void pixelsMatrixToFloatArray(const cv::Mat& tile,
                                      const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>>& tensor) const;
};


#endif //SUPERIMAGE_UPSCALINGENGINE_H
