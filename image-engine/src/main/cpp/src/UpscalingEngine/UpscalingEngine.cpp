//
// Created by Zhenxiang Chen on 24/08/23.
//

#include <chrono>

#include <UpscalingEngine/image_tile_interpreter.h>
#include <UpscalingEngine/UpscalingEngine.h>
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

void UpscalingEngine::pixelsMatrixToFloatArray(const cv::Mat& tile,
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
            tensor(0, x, y) = pixel[0] / 255.0f;
            // Green
            tensor(1, x, y) = pixel[1] / 255.0;
            // Blue for Android, Red for desktop
            tensor(2, x, y) = pixel[2] / 255.0;
        }
    }
}

void output_tensor_to_pixels_matrix(
        cv::Mat& matrix,
        const Eigen::Tensor<float, 3, Eigen::RowMajor>& tensor) {

    for (int y = 0; y < tensor.dimension(2); y++) {
        for (int x = 0; x < tensor.dimension(1); x++) {
            // When we have RGB values, we pack them into output_tile single pixel.
            // Format is ABGR for android, and ARGB for desktop
            // Assume little endian order since this will only run on ARM and x86
            const uint8_t r = std::clamp<float>(tensor(0, x, y) * 255, 0, 255);
            const uint8_t g = std::clamp<float>(tensor(1, x, y) * 255, 0, 255);
            const uint8_t b = std::clamp<float>(tensor(2, x, y) * 255, 0, 255);
            matrix.at<cv::Vec4b>(y, x) = {r, g, b, UCHAR_MAX};
        }
    }
}

Eigen::Tensor<float, 3, Eigen::RowMajor> trim_tensor_padding(
        const int scale,
        const std::pair<int, int> x_padding,
        const std::pair<int, int> y_padding,
        const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>>* tensor) {

    Eigen::array<Eigen::Index, 3> offsets = {
            0,
            x_padding.first * scale,
            y_padding.first * scale};
    Eigen::array<Eigen::Index, 3> extents = {
            tensor->dimension(0),
            tensor->dimension(1) - x_padding.first * scale - x_padding.second * scale,
            tensor->dimension(2) - y_padding.first * scale - y_padding.second * scale};

    return tensor->slice(offsets, extents);
}

std::pair<int, int> calculate_axis_padding(const int position, const int axis_size, const int tile_size, const int padding) {
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

void UpscalingEngine::upscaleImage(
        JNIEnv *jni_env,
        jobject progress_tracker,
        jobject coroutine_scope,
        cv::Mat &input_image_matrix,
        cv::Mat &output_image_matrix) {

    const auto start = std::chrono::high_resolution_clock::now();

    int y = 0;
    int last_row_height = 0;
    size_t processed_pixels = 0;
    set_progress_percentage(jni_env, progress_tracker, 0);

    // Consider provided tile size only if more than 0
    const image_dimensions tile_dimensions = (tileSize > 0) ? image_dimensions{
            // Adapt tile size if image size is smaller than default tile size
            std::min<int>(tileSize, input_image_matrix.cols),
            std::min<int>(tileSize, input_image_matrix.rows)
    } : image_dimensions { input_image_matrix.cols, input_image_matrix.rows };
    const int height = input_image_matrix.rows;
    const int width = input_image_matrix.cols;

    interpreter.updateTileSize(&tile_dimensions);

    const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> input_tensor(
            interpreter.input_buffer,
            REALESRGAN_IMAGE_CHANNELS,
            tile_dimensions.width,
            tile_dimensions.height);

    const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> output_tensor(
            interpreter.output_buffer,
            REALESRGAN_IMAGE_CHANNELS,
            tile_dimensions.width * scale,
            tile_dimensions.height * scale);

    while (is_coroutine_scope_active(jni_env, coroutine_scope)) {
        int x = 0;
        std::pair<int, int> y_padding = calculate_axis_padding(y, height, tile_dimensions.height, REALESRGAN_INPUT_TILE_PADDING);

        while (is_coroutine_scope_active(jni_env, coroutine_scope)) {

            std::pair<int, int> x_padding = calculate_axis_padding(x, width, tile_dimensions.width, REALESRGAN_INPUT_TILE_PADDING);

            // Get input_tile of pixels to process keeping, apply left padding as offset that will be cropped later
            const cv::Mat input_tile = input_image_matrix(
                    cv::Rect(
                            x - x_padding.first,
                            y - y_padding.first,
                            tile_dimensions.width,
                            tile_dimensions.height));

            // Feed input into tensor
            pixelsMatrixToFloatArray(input_tile, input_tensor);

            // Run inference on the model
            interpreter.inference();

            const Eigen::Tensor<float, 3, Eigen::RowMajor> cropped_output_tensor = trim_tensor_padding(
                    scale,
                    x_padding,
                    y_padding,
                    &output_tensor);

            cv::Mat output_dest_block = output_image_matrix(cv::Rect(
                    x * scale,
                    y * scale,
                    cropped_output_tensor.dimension(1),
                    cropped_output_tensor.dimension(2)));

            output_tensor_to_pixels_matrix(output_dest_block, cropped_output_tensor);

            // Update progress
            processed_pixels += output_dest_block.total();
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
            x += output_dest_block.cols / scale;
            if (x == width) {
                last_row_height = output_dest_block.rows / scale;
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

UpscalingEngine::UpscalingEngine(const char *model_path,
                                 const int scale,
                                 const uint32_t tile_size,
                                 const int32_t placeholderColour) : interpreter(model_path), scale(scale), tileSize(tile_size), placeholderColour(
        placeholderColour) {
}

UpscalingEngine::~UpscalingEngine() = default;
