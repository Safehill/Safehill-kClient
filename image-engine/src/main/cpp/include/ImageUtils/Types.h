//
// Created by Zhenxiang Chen on 02/03/23.
//

#include <Eigen/Core>

#ifndef SUPERIMAGE_TYPES_H
#define SUPERIMAGE_TYPES_H

using PixelMatrix = Eigen::Map<Eigen::Matrix<int32_t, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>;

struct image_dimensions {
    int width;
    int height;
};

#endif //SUPERIMAGE_TYPES_H
