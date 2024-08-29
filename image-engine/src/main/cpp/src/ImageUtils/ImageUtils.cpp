//
// Created by Zhenxiang Chen on 09/12/23.
//

#include "ImageUtils/ImageUtils.h"
#include <stdexcept>

cv::Mat ImageUtils::loadImageAsRGBAMat(const char *filename) {
    int readFlags = cv::IMREAD_UNCHANGED;
    // Workaround to apply exif transformation with the dumpster-fire imread flags logic
    readFlags &= ~cv::IMREAD_IGNORE_ORIENTATION;
    cv::Mat mat = cv::imread(filename, readFlags);
    int code;

    switch (mat.channels()) {
        case 1:
            code = cv::COLOR_GRAY2RGBA;
            break;
        case 3:
            code = cv::COLOR_BGR2RGBA;
            break;
        case 4:
            code = cv::COLOR_BGRA2RGBA;
            break;
        default:
            throw std::runtime_error("Unsupported channel count " + std::to_string(mat.channels()));
    }

    cv::Mat colorConverted;
    cv::cvtColor(mat, colorConverted, code);
    mat.release();

    cv::Mat premultipliedMat;
    cv::cvtColor(colorConverted, premultipliedMat, cv::COLOR_RGBA2mRGBA);

    return premultipliedMat;
}

bool ImageUtils::writeImageRGBAMat(const char *filename, const cv::Mat &mat, OutputFormat format) {
    cv::Mat demultipliedMat;
    cv::cvtColor(mat, demultipliedMat, cv::COLOR_mRGBA2RGBA);

    cv::Mat bgraMat;
    cv::cvtColor(demultipliedMat, bgraMat, cv::COLOR_RGBA2BGRA);

    std::vector<int> params;

    switch (format) {
        case JPEG:
            params = { cv::IMWRITE_JPEG_QUALITY, 100 };
            break;
        case PNG:
            params = { cv::IMWRITE_PNG_COMPRESSION, 5 };
            break;
    }

    return cv::imwrite(filename, bgraMat, params);
}
