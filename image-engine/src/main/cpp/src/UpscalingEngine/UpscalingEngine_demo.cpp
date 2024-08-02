#include <cxxopts.hpp>
#include <iostream>
#include <filesystem>

#include "ImageUtils/ImageUtils.h"
#include "UpscalingEngine/UpscalingEngine.h"

bool createDirectoryIfNotExists(const std::string& directoryPath) {
    std::filesystem::path path(directoryPath);

    if (std::filesystem::exists(path)) {
        return true;
    } else {
        return std::filesystem::create_directories(path);
    }
}


void processImage(
    const std::filesystem::path &inputImagePath,
    UpscalingEngine &engine,
    std::filesystem::path &outputDir) {

    auto inputMat = ImageUtils::loadImageAsRGBAMat(std::filesystem::absolute(inputImagePath).c_str());
    auto outputMat = cv::Mat(inputMat.rows * engine.scale, inputMat.cols * engine.scale, inputMat.type());
    const auto outputImagePath = std::filesystem::absolute(outputDir / inputImagePath.filename());

    engine.upscaleImage(nullptr, nullptr, nullptr, inputMat, outputMat);

    ImageUtils::writeImageRGBAMat(outputImagePath.c_str(), outputMat, ImageUtils::OutputFormat::PNG);
}

int main(int argc, char** argv) {
    cxxopts::Options options("UpscalingEngine_demo", "UpscalingEngine demo");

    options.add_options()
        ("i,input", "Input image file", cxxopts::value<std::string>())
        ("m,model", "Upscaling model file in MNN format", cxxopts::value<std::string>())
        ("s,scale", "The scaling factor of the model", cxxopts::value<uint32_t>())
        ("o,output", "Output directory", cxxopts::value<std::string>())
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
    const auto outputDir = result["output"].as<std::string>();
    const auto tileSize = result["tile-size"].as<std::int32_t>();

    auto upscalingEngine = UpscalingEngine(model.c_str(), scale, tileSize, INT32_MAX);

    try {
        if (!createDirectoryIfNotExists(outputDir)) {
            std::cout<<"Failed to create output directory"<<std::endl;
            return -1;
        }
    } catch (const std::filesystem::filesystem_error& e) {
        std::cout<<"Failed to create output directory"<<std::endl;
        return -1;
    }

    std::filesystem::path directoryPath(input);
    std::filesystem::path outputPath(outputDir);
    for (const auto& entry : std::filesystem::directory_iterator(directoryPath)) {
        if (entry.is_regular_file()) {
            processImage(entry.path(), upscalingEngine, outputPath);
        }
    }

    return 0;
}