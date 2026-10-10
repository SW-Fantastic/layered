#ifdef _WIN32
    #ifndef NOMINMAX
    #define NOMINMAX
    #endif
    
    #include<iostream>
    #include <Windows.h>
    #include "windows/jni.h"
   
    std::unique_ptr<wchar_t[]> toPlatformStr(const char* str);

#elif __APPLE__
    #include <dlfcn.h>
    #include "osx/jni.h"
#else
    #include <dlfcn.h>
    #include "linux/jni.h"
#endif


