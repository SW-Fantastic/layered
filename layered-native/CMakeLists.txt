cmake_minimum_required(VERSION 3.15)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

#定义 vcpkg 子模块路径
set(VCPKG_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/vcpkg" CACHE PATH "Vcpkg root dir" FORCE)
#对库的编译参数进行微调
set(VCPKG_OVERLAY_PORTS "${CMAKE_CURRENT_SOURCE_DIR}/overlay" CACHE PATH "Custom vcpkg ports" FORCE)
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
        if(NOT EXISTS "${VCPKG_ROOT}/.vcpkg-root")
          file(WRITE "${VCPKG_ROOT}/.vcpkg-root" "")
        endif()
        execute_process(COMMAND "${VCPKG_ROOT}/bootstrap-vcpkg.sh" WORKING_DIRECTORY "${VCPKG_ROOT}")
    endif()
endif()



#配置工具链为项目内置的工具链文件
set(CMAKE_TOOLCHAIN_FILE "${VCPKG_ROOT}/scripts/buildsystems/vcpkg.cmake"
    CACHE STRING "Vcpkg toolchain file" FORCE)

set(VCPKG_FEATURE_FLAGS "-binarycaching")

#Vcpkg静态C++运行时的triplet配置
if(NOT VCPKG_TARGET_TRIPLET)
    if(WIN32)
        set(VCPKG_TARGET_TRIPLET "x64-windows-static" CACHE STRING "" FORCE)
    else()
        # Linux / macOS
        set(VCPKG_TARGET_TRIPLET "x64-linux" CACHE STRING "" FORCE) # Linux 默认就是共享系统 libc 但可以静态 C++ 
    endif()
endif()

project(LayerTools)

find_package(LibXml2 REQUIRED)
find_package(ZLIB REQUIRED)
find_package(zstd REQUIRED)
find_package(LLVM REQUIRED CONFIG)
find_package(Clang REQUIRED CONFIG)
find_package(PkgConfig REQUIRED)
find_package(mimalloc REQUIRED CONFIG)
find_package(unofficial-libffi CONFIG REQUIRED)

include_directories(
    ${JNI_INCLUDE_DIRS}
    ${LLVM_INCLUDE_DIRS}
    ${CLANG_INCLUDE_DIRS}
)
add_definitions(${LLVM_DEFINITIONS})

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

file(GLOB _SRC src/*.cpp src/*.c)
file(GLOB _LIBSRC src/*.cpp src/*.c src/lib/*.cpp)
file(GLOB _RUNTIME src/runtime/*.cpp)
add_library(layerRuntime SHARED ${_RUNTIME})

include_directories("include")
add_library(layer SHARED ${_LIBSRC})
add_executable(layer_test ${_SRC})

llvm_map_components_to_libnames(LLVM_SUPPORT 
    support 
    core 
    frontenddriver
    option
)

set(LLVM_TOOLING
    clangTooling
    clangFrontend
    clangDriver
    clangSerialization
    clangParse
    clangSema
    clangAnalysis
    clangAST
    clangBasic
    clangEdit
    clangLex
)

# 🛠️ 跨平台 C++ 运行时 (CRT) 静态链接统一配置
if(WIN32)
    # -----------------------------------------------------------------
    # Windows 平台：强制所有 Target 使用 /MT 或 /MTd
    # -----------------------------------------------------------------
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>" CACHE STRING "" FORCE)
    message(STATUS "Using Static CRT for Windows (/MT)")
else()
    # -----------------------------------------------------------------
    # Linux / macOS / Android (GCC 或 Clang) 平台
    # -----------------------------------------------------------------
    # 强制链接器静态合并标准 C++ 库和 GCC 基础库
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libstdc++ -static-libgcc")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static-libstdc++ -static-libgcc")
    message(STATUS "Using Static C++ Runtime for Unix-like platform")
endif()

target_link_libraries(layer PRIVATE
        ${LLVM_TOOLING}
        ${LLVM_SUPPORT}
)
target_link_libraries(layer_test PRIVATE
        ${LLVM_TOOLING}
        ${LLVM_SUPPORT}
)

target_link_libraries(layerRuntime PRIVATE unofficial::libffi::libffi)
target_link_libraries(layerRuntime PRIVATE mimalloc)