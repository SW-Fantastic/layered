#ifdef _WIN32
    #ifndef NOMINMAX
    #define NOMINMAX
    #endif
    #include <Windows.h>
    #include "windows/jni.h"
#elif __APPLE__
    #include <dlfcn.h>
    #include "osx/jni.h"
#else
    #include <dlfcn.h>
    #include "linux/jni.h"
#endif


