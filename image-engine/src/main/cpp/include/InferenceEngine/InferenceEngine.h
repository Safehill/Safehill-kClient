//
// Created by Zhenxiang Chen on 24/08/23.
//

#ifndef SAFEHILL_INFERENCEENGINE_H
#define SAFEHILL_INFERENCEENGINE_H

#include <jni.h>
#include <opencv2/opencv.hpp>
#include "unsupported/Eigen/CXX11/Tensor"

#include "ImageUtils/Types.h"
#include "image_tile_interpreter.h"

#define REALESRGAN_INPUT_TILE_PADDING 10

class InferenceEngine {

public:
    InferenceEngine(const char* model_path,
                    const int scale,
                    const int tile_size,
                    const int32_t placeholderColour);
    ~InferenceEngine();

    const int scale;

    const int tileSize;

    /**
     * Colour that will replace a transparent pixel since Real-ESRGAN doesn't support upscaling
     */
    const int32_t placeholderColour;

    void runInference(
            JNIEnv* jni_env,
            jobject progress_tracker,
            jobject coroutine_scope,
            cv::Mat &input_image_matrix,
            cv::Mat &output_image_matrix);

private:
    ImageTileInterpreter interpreter;

    void pixelsMatrixToFloatArray(const cv::Mat& tile,
                                      const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>>& tensor) const;

    static std::pair<int, int> calculateInputTilePadding(int position, int axis_size, int tile_size, int padding);

    static cv::Size copyTensorToMatRegion(
            cv::Size size,
            cv::Point2i position,
            std::pair<int, int> xPadding,
            std::pair<int, int> yPadding,
            cv::Mat &imageMat,
            const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>>& tensor);

    static cv::Vec4b getColourAt(
            cv::Point2i position,
            const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>>& tensor);
};


#endif //SAFEHILL_INFERENCEENGINE_H
