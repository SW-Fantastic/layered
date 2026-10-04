cmake_minimum_required(VERSION 3.12)
project([=projectName])

# 跨平台 C++ 运行时 (CRT) 静态链接统一配置
if(WIN32)
    # -----------------------------------------------------------------
    # Windows 平台：强制所有 Target 使用 /MT 或 /MTd
    # -----------------------------------------------------------------
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>" CACHE STRING "" FORCE)
    message(STATUS "Using Static CRT for Windows (/MT)")
else()
    # -----------------------------------------------------------------
    # Linux / macOS (GCC 或 Clang) 平台
    # -----------------------------------------------------------------
    # 强制链接器静态合并标准 C++ 库和 GCC 基础库
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libstdc++ -static-libgcc")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static-libstdc++ -static-libgcc")
    message(STATUS "Using Static C++ Runtime for Unix-like platform")
endif()

# 定义用户传入的多个库查找目录（用分号分隔的列表）
# 默认指向当前目录下的 libs 文件夹
set(SEARCH_LIB_DIRS 
    "${CMAKE_CURRENT_SOURCE_DIR}/libs" 
    [=getLibrarySearchPathStr()] 
)

# 定义需要查找并链接的第三方库名称（不需要写前缀如 lib 或后缀如 .a/.so/.lib）
set(REQUIRED_LIBS [=getLibraryNamesStr()])

# 循环查找每一个库，并将其保存到变量中
set(LIBS_LIST "")
foreach(LIB_NAME ${REQUIRED_LIBS})
    # 清除上一次循环的缓存（确保多库查找不会互相干扰）
    unset(FOUND_LIB CACHE)
    
    # 在指定的多个目录中查找特定的库
    find_library(FOUND_LIB
        NAMES ${LIB_NAME}
        PATHS ${SEARCH_LIB_DIRS}
        NO_DEFAULT_PATH # 如果你只想在指定的目录找，取消这行的注释；想同时找系统路径则删掉这行
    )
    
    if(FOUND_LIB)
        message(STATUS "Found library ${LIB_NAME}: ${FOUND_LIB}")
        list(APPEND LIBS_LIST ${FOUND_LIB})
    else()
        message(FATAL_ERROR "Could not find required library: ${LIB_NAME} in paths: ${SEARCH_LIB_DIRS}")
    endif()

endforeach()

# 收集源文件
file(GLOB LIST_SOURCES CONFIGURE_DEPENDS 
    "${CMAKE_CURRENT_SOURCE_DIR}/src/*.cpp" 
    "${CMAKE_CURRENT_SOURCE_DIR}/src/*.c"
)

# 添加Wrapper的Target
add_library([=getProjectTargetName()] SHARED ${LIST_SOURCES})

# 这里是include目录
target_include_directories([=getProjectTargetName()] PRIVATE 
    "${CMAKE_CURRENT_SOURCE_DIR}/include" 
    [=getIncludeDirectoriesStr()]
)

# 链接查找到的第三方库
# CMake 会自动处理 .lib, .so, .a, .dylib 甚至 macOS 的 .framework
target_link_libraries([=getProjectTargetName()] PRIVATE ${LIBS_LIST})