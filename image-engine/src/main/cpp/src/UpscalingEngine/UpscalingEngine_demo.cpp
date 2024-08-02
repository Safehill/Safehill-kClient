#include <cxxopts.hpp>
#include <iostream>

#include "ImageUtils/ImageUtils.h"
#include "UpscalingEngine/UpscalingEngine.h"

int main(int argc, char** argv) {
    cxxopts::Options options("UpscalingEngine_demo", "UpscalingEngine demo");

    options.add_options()
        ("i,input", "Input image file", cxxopts::value<std::string>())
        ("m,model", "Upscaling model file in MNN format", cxxopts::value<std::string>())
        ("s,scale", "The scaling factor of the model", cxxopts::value<uint32_t>())
        ("o,output", "Output image file", cxxopts::value<std::string>())
        ("t,tile-size", "Tile size used in processing, specify 0 or negative to disable tiling", cxxopts::value<std::int32_t>())
        ("h,help", "Print usage")
    ;
    auto result = options.parse(argc, argv);

    if (result.count("help")) {
        std::cout << options.help() << std::endl;
        exit(0);
    }

    const auto input = result["input"].as<std::string>();
    const auto model = result["model"].as<std::string>();
    const auto scale = result["scale"].as<uint32_t>();
    const auto output = result["output"].as<std::string>();
    const auto tileSize = result["tile-size"].as<std::int32_t>();

    auto inputMat = ImageUtils::loadImageAsRGBAMat(input.c_str());
    auto outputMat = cv::Mat(inputMat.rows * scale, inputMat.cols * scale, inputMat.type());
    auto upscalingEngine = UpscalingEngine(model.c_str(), scale, tileSize, INT32_MAX);

    upscalingEngine.upscaleImage(nullptr, nullptr, nullptr, inputMat, outputMat);

    ImageUtils::writeImageRGBAMat(output.c_str(), outputMat, ImageUtils::OutputFormat::PNG);

    return 0;
}