cmake_minimum_required(VERSION 3.22.1)
project(guise_vulkan LANGUAGES CXX)

add_library(guise_vulkan SHARED vulkan_privacy.cpp)

target_compile_features(guise_vulkan PRIVATE cxx_std_17)
target_compile_options(
    guise_vulkan
    PRIVATE
    -Wall
    -Wextra
    -Werror
    -fvisibility=hidden
)

target_link_libraries(guise_vulkan PRIVATE dl)
