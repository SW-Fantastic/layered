#define JEMALLOC_NO_RENAME
#include<jemalloc/jemalloc.h>

#ifdef _WIN32
    #include <Windows.h>
    #include "windows/jni.h"
#elif __APPLE__
    #include <dlfcn.h>
    #include "osx/jni.h"
#else
    #include <dlfcn.h>
    #include "linux/jni.h"
#endif

void* asWString(const char* javaStr);

