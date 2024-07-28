//
// Created by Zhenxiang Chen on 09/12/23.
//

#ifndef SUPERIMAGE_IMAGEUTILS_H
#define SUPERIMAGE_IMAGEUTILS_H

#include <opencv2/opencv.hpp>

class ImageUtils {

public:

    enum OutputFormat {
        JPEG = 0,
        PNG = 1
    };

    static cv::Mat loadImageAsRGBAMat(const char* filename);

    static bool writeImageRGBAMat(const char* filename, const cv::Mat &mat, OutputFormat format);
};

#endif //SUPERIMAGE_IMAGEUTILS_H
