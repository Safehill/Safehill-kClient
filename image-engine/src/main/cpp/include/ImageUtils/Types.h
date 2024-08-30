//
// Created by Zhenxiang Chen on 02/03/23.
//

#include <Eigen/Core>

#ifndef SAFEHILL_TYPES_H
#define SAFEHILL_TYPES_H

using PixelMatrix = Eigen::Map<Eigen::Matrix<int32_t, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>;

struct image_dimensions {
    int width;
    int height;
};

#endif //SAFEHILL_TYPES_H
