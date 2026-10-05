#ifndef _[=getPrefix()]_H_
#define _[=getPrefix()]_H_

#include<stdint.h>
#include <cstddef>
<#list getIncludes() as includeItem>
#include"[=includeItem]"
</#list>

#ifdef _WIN32
    #define _[=getPrefix()]_API __declspec(dllexport)
#else
    #define _[=getPrefix()]_API __attribute__((visibility("default")))
#endif

extern "C" {

<#list getDeclares()?keys as declare>
_[=getPrefix()]_API [=getDeclares()[declare]]

</#list>

}

#endif