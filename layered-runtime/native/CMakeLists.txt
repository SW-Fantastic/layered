
cmake_minimum_required(VERSION 3.15)

#定义 vcpkg 子模块路径
set(VCPKG_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/vcpkg")
#如果 vcpkg.exe 不存在，自动运行 bootstrap
if(WIN32)
    set(VCPKG_EXECUTABLE "${VCPKG_ROOT}/vcpkg.exe")
else()
    set(VCPKG_EXECUTABLE "${VCPKG_ROOT}/vcpkg")
endif()

if(NOT EXISTS "${VCPKG_EXECUTABLE}")
    message(STATUS "vcpkg (Bootstrap)...")
    if(WIN32)
        execute_process(COMMAND "${VCPKG_ROOT}/bootstrap-vcpkg.bat" WORKING_DIRECTORY "${VCPKG_ROOT}")
    else()
        execute_process(COMMAND "${VCPKG_ROOT}/bootstrap-vcpkg.sh" WORKING_DIRECTORY "${VCPKG_ROOT}")
    endif()
endif()

#配置工具链为项目内置的工具链文件
set(CMAKE_TOOLCHAIN_FILE "${VCPKG_ROOT}/scripts/buildsystems/vcpkg.cmake"
    CACHE STRING "Vcpkg toolchain file" FORCE)

project(liblayer)

file(GLOB _SRC src/*.cpp)
include_directories("include")
add_library(liblayer SHARED ${_SRC})
# using Vcpkg for libffi
find_package(PkgConfig REQUIRED)
pkg_check_modules(JEMALLOC REQUIRED IMPORTED_TARGET jemalloc)
find_package(unofficial-libffi CONFIG REQUIRED)
target_link_libraries(liblayer PRIVATE unofficial::libffi::libffi)
target_link_libraries(liblayer PRIVATE PkgConfig::JEMALLOC)