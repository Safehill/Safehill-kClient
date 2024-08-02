cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.
project("Safehill-ImageEngine")

set(EIGEN_INCLUDE "${CMAKE_CURRENT_SOURCE_DIR}/eigen")
set(MNN_INCLUDE "${CMAKE_CURRENT_SOURCE_DIR}/MNN/include")
set(CXXOPTS_INCLUDE "${CMAKE_CURRENT_SOURCE_DIR}/cxxopts/include")
set(
    OPENCV_INCLUDES
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/include
    ${CMAKE_CURRENT_BINARY_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/calib3d/include
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/core/include
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/features2d/include
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/flann/include
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/highgui/include
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/imgcodecs/include
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/imgproc/include
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/objdetect/include
    ${CMAKE_CURRENT_SOURCE_DIR}/opencv/modules/stitching/include)

set(CMAKE_CXX_STANDARD 20)

if(ANDROID)
    find_library(jnigraphics-lib jnigraphics)

    add_library(BitmapUtils SHARED src/BitmapUtils/AndroidBitmapWrapper.cpp)
    target_include_directories(BitmapUtils PUBLIC ${EIGEN_INCLUDE} ${OPENCV_INCLUDES})
    target_link_libraries(BitmapUtils ${jnigraphics-lib} opencv_core)

    set(PLATFORM_LIBRARIES BitmapUtils android)
else()
    find_package(JNI REQUIRED)
    include_directories(${JNI_INCLUDE_DIRS})

    set(PLATFORM_LIBRARIES ImageUtils)
    add_library(ImageUtils SHARED src/ImageUtils/ImageUtils.cpp)
    target_include_directories(ImageUtils PUBLIC ${OPENCV_INCLUDES})
    target_link_libraries(ImageUtils opencv_imgcodecs)
endif()

if (WIN32)
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
endif()

# MNN options
if (APPLE)
    option(MNN_METAL "" ON)
else()
    # option(MNN_VULKAN "" ON)
endif()
if (WIN32)
    option(MNN_CUDA "" ON)
    option(MNN_WIN_RUNTIME_MT "" ON)
endif()
# option(MNN_OPENCL "" ON)

# Include MNN in build
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/MNN)

# OpenCV options
option(BUILD_opencv_calib3d "" ON)
option(BUILD_opencv_dnn "" OFF)
option(BUILD_opencv_features2d "" ON)
option(BUILD_opencv_flann "" ON)
option(BUILD_opencv_gapi "" OFF)
option(BUILD_opencv_highgui "" ON)
option(BUILD_opencv_imgcodecs "" ON)
option(BUILD_opencv_imgproc "" ON)
option(BUILD_opencv_ml "" OFF)
option(BUILD_opencv_photo "" OFF)
option(BUILD_opencv_video "" OFF)
option(BUILD_opencv_videoio "" OFF)
option(WITH_PROTOBUF "" OFF)
option(BUILD_JAVA "" OFF)
option(BUILD_ANDROID_PROJECTS "" OFF)
option(BUILD_ANDROID_EXAMPLES "" OFF)
option(BUILD_KOTLIN_EXTENSIONS "" OFF)

# Include OpenCV in build
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/opencv)

include_directories(include)

# JNI utils
add_library(JNIUtils SHARED src/JNIUtils/JNIUtils.cpp)

# MNN helper
add_library(MNNHelper SHARED src/MNNHelper/MNNHelper.cpp)
target_include_directories(MNNHelper PRIVATE ${MNN_INCLUDE})
target_link_libraries(MNNHelper MNN)

# Upscaling Engine
add_library(
        UpscalingEngine
        SHARED
        src/UpscalingEngine/coroutine_utils.cpp
        src/UpscalingEngine/progress_tracker.cpp
        src/UpscalingEngine/image_tile_interpreter.cpp
        src/UpscalingEngine/UpscalingEngine.cpp
        src/UpscalingEngine/UpscalingEngineJNI.cpp)
target_include_directories(UpscalingEngine PUBLIC ${OPENCV_INCLUDES} ${EIGEN_INCLUDE} ${MNN_INCLUDE})
target_link_libraries(UpscalingEngine ${PLATFORM_LIBRARIES} opencv_imgproc JNIUtils MNNHelper)

# Upscaling Engine - demo
add_executable(UpscalingEngine_demo src/UpscalingEngine/UpscalingEngine_demo.cpp)
target_include_directories(UpscalingEngine_demo PRIVATE ${CXXOPTS_INCLUDE})
target_link_libraries(UpscalingEngine_demo PRIVATE UpscalingEngine)
