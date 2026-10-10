#ifndef _LAYER_METADATA_H_
#define _LAYER_METADATA_H_

#include <cstddef>
#include<stdint.h>
<#list includes as includeItem>
#include"[=includeItem]"
</#list>

#ifdef _WIN32
    #define _LAYER_META_API __declspec(dllexport)
#else
    #define _LAYER_META_API __attribute__((visibility("default")))
#endif

extern "C" {

_LAYER_META_API const unsigned char* getMetaData();

_LAYER_META_API const int getMetaDataSize();

}

#endif