//
// Created by Zhenxiang Chen on 24/08/23.
//

#include <chrono>

#include <InferenceEngine/image_tile_interpreter.h>
#include <InferenceEngine/InferenceEngine.h>
#include "coroutine_utils.h"
#include "progress_tracker.h"

// Must be premultiplied !!!
cv::Vec4b alphaComposition(const cv::Vec4b srcColour, const int32_t destColour) {
    // Extract RGBA components from the source and destination colors
    const uint8_t srcAlpha = srcColour[3];

    const uint8_t destRed   = destColour & 0xFF;
    const uint8_t destGreen = (destColour >> 8) & 0xFF;
    const uint8_t destBlue  = (destColour >> 16) & 0xFF;
    const uint8_t destAlpha = (destColour >> 24) & 0xFF;

    // Calculate the new alpha value after compositing
    const uint8_t newAlpha = srcAlpha + destAlpha * (255 - srcAlpha) / 255;

    // Calculate the new color values after compositing (no separate alpha scaling)
    const uint8_t newRed   = srcColour[0] + destRed * (255 - srcAlpha) / 255;
    const uint8_t newGreen = srcColour[1] + destGreen * (255 - srcAlpha) / 255;
    const uint8_t newBlue  = srcColour[2] + destBlue * (255 - srcAlpha) / 255;

    // Combine the new color values and alpha to form the resulting color
    return {newRed, newGreen, newBlue, newAlpha};
}

void InferenceEngine::pixelsMatrixToFloatArray(const cv::Mat& tile,
                                               const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>>& tensor) const {

    // Convert input image int array to float array
    // Format is ABGR for android, and ARGB for desktop
    // with tensor shape [1, REALESRGAN_IMAGE_CHANNELS, tile height, tile width]
    for (int y = 0; y < tile.rows; y++) {
        for (int x = 0; x < tile.cols; x++) {
            auto pixel = tile.at<cv::Vec4b>(y, x);
            // Blend translucent pixels with placeholder colour, since only upscaling solid colours
            // is supported
            if (pixel[3] < 0xff) {
                pixel = alphaComposition(pixel, placeholderColour);
            }
            // Red for Android, Blue for desktop
            tensor(0, y, x) = pixel[0] / 255.0f;
            // Green
            tensor(1, y, x) = pixel[1] / 255.0;
            // Blue for Android, Red for desktop
            tensor(2, y, x) = pixel[2] / 255.0;
        }
    }
}

std::pair<int, int> InferenceEngine::calculateInputTilePadding(
        const int position,
        const int axis_size,
        const int tile_size,
        const int padding) {
    if (axis_size == tile_size) {
        // No padding needed if there a single tile for given axis
        return std::pair<int, int> {0, 0};
    } else if (position == 0) {
        // First tile
        return std::pair<int, int> {0, padding};
    } else if (axis_size - position <= tile_size - padding) {
        // Final tile
        return std::pair<int, int> {(tile_size - (axis_size - position)), 0};
    } else {
        // Tiles in between
        return std::pair<int, int> {padding, padding};
    }
}

void InferenceEngine::runInference(
        JNIEnv *jni_env,
        jobject progress_tracker,
        jobject coroutine_scope,
        cv::Mat &input_image_matrix,
        cv::Mat &output_image_matrix) {

    const auto start = std::chrono::high_resolution_clock::now();
    const int height = input_image_matrix.rows;
    const int width = input_image_matrix.cols;

    int y = 0;
    int last_row_height = 0;
    size_t processed_pixels = 0;
    set_progress_percentage(jni_env, progress_tracker, 0);

    // Consider provided tile size only if more than 0
    const image_dimensions tile_dimensions = (tileSize > 0) ? image_dimensions{
            // Adapt tile size if image size is smaller than default tile size
            std::min<int>(tileSize, width),
            std::min<int>(tileSize, height)
    } : image_dimensions { width, height };

    interpreter.updateTileSize(&tile_dimensions);

    const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> inputTensorMap(
            interpreter.input_tensor->host<float>(),
            interpreter.input_tensor->channel(),
            interpreter.input_tensor->height(),
            interpreter.input_tensor->width());

    const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> outputTensorMap(
            interpreter.output_tensor->host<float>(),
            interpreter.output_tensor->channel(),
            interpreter.output_tensor->height(),
            interpreter.output_tensor->width());

    while (is_coroutine_scope_active(jni_env, coroutine_scope)) {
        int x = 0;
        std::pair<int, int> y_padding = calculateInputTilePadding(y, height, tile_dimensions.height,
                                                                  REALESRGAN_INPUT_TILE_PADDING);

        while (is_coroutine_scope_active(jni_env, coroutine_scope)) {

            std::pair<int, int> x_padding = calculateInputTilePadding(x, width,
                                                                      tile_dimensions.width,
                                                                      REALESRGAN_INPUT_TILE_PADDING);

            // Get input_tile of pixels to process keeping, apply left padding as offset that will be cropped later
            const cv::Mat input_tile = input_image_matrix(
                    cv::Rect(
                            x - x_padding.first,
                            y - y_padding.first,
                            tile_dimensions.width,
                            tile_dimensions.height));

            // Feed input into tensor
            pixelsMatrixToFloatArray(input_tile, inputTensorMap);

            // Run inference on the model
            interpreter.inference();

            const cv::Size copiedBlockSize = copyTensorToMatRegion(
                    cv::Size(tile_dimensions.width * scale, tile_dimensions.height * scale),
                    cv::Point2i(x * scale, y * scale),
                    {x_padding.first * scale, x_padding.second * scale},
                    {y_padding.first * scale, y_padding.second * scale},
                    output_image_matrix,
                    outputTensorMap);

            // Update progress
            processed_pixels += copiedBlockSize.area();
            const float progress = 100 * ((float)processed_pixels / output_image_matrix.total());
            // Calculate execution time per 1%
            const double percentage_execution_millis = std::chrono::duration<double, std::milli>(
                    std::chrono::high_resolution_clock::now() - start).count() / progress;
            set_progress_percentage(
                    jni_env,
                    progress_tracker,
                    progress,
                    static_cast<int64_t>(round((100 - progress) * percentage_execution_millis)));

            // Recalculate padding and position of next input_tile in row
            x += copiedBlockSize.width / scale;
            if (x == width) {
                last_row_height = copiedBlockSize.height / scale;
                break;
            }
        }

        y += last_row_height;
        // Recalculate padding and position of next column's tiles
        if (y == height) {
            break;
        }
    }
}

InferenceEngine::InferenceEngine(const char *model_path,
                                 const int scale,
                                 const int tile_size,
                                 const int32_t placeholderColour) : interpreter(model_path), scale(scale), tileSize(tile_size), placeholderColour(
        placeholderColour) {
}

cv::Size InferenceEngine::copyTensorToMatRegion(cv::Size size,
                                                cv::Point2i position,
                                                std::pair<int, int> xPadding,
                                                std::pair<int, int> yPadding,
                                                cv::Mat &imageMat,
                                                const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> &tensor) {
    const cv::Size destBlockSize(
            size.width - xPadding.first - xPadding.second,
            size.height - yPadding.first - yPadding.second);
    cv::Mat destBlockMat = imageMat(cv::Rect(position, destBlockSize));
    const cv::Size croppedTileSize(
            tensor.dimension(2) - xPadding.first - xPadding.second,
            tensor.dimension(1) - yPadding.first - yPadding.second);

    if (destBlockSize == croppedTileSize) {
        // Trim padding from tensor and copy to destination block
        for (int y = 0; y < croppedTileSize.height; y++) {
            for (int x = 0; x < croppedTileSize.width; x++) {
                destBlockMat.at<cv::Vec4b>(y, x) =
                        getColourAt(cv::Point2i(x + xPadding.first, y + yPadding.first), tensor);
            }
        }
    } else {
        // Handle cases where output tensor size is slightly different than expected destination
        // block, by using a intermediate cv::Mat matching the tensor size and then interpolate to
        // the correct destination block size
        cv::Mat croppedTensorMat(croppedTileSize, destBlockMat.type());

        // Trim padding from tensor and copy to intermediate cv::Mat
        for (int y = 0; y < croppedTileSize.height; y++) {
            for (int x = 0; x < croppedTileSize.width; x++) {
                croppedTensorMat.at<cv::Vec4b>(y, x) =
                        getColourAt(cv::Point2i(x + xPadding.first, y + yPadding.first), tensor);
            }
        }
        cv::resize(croppedTensorMat, destBlockMat, destBlockSize, 0, 0, cv::INTER_NEAREST);
    }

    return destBlockSize;
}

cv::Vec4b InferenceEngine::getColourAt(cv::Point2i position,
                                       const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> &tensor) {

    return cv::Vec4b(
            // Red
            std::clamp<float>(tensor(0, position.y, position.x) * 255, 0, 255),
            // Green
            std::clamp<float>(tensor(1, position.y, position.x) * 255, 0, 255),
            // Blue
            std::clamp<float>(tensor(2, position.y, position.x) * 255, 0, 255),
            UCHAR_MAX);
}

InferenceEngine::~InferenceEngine() = default;
