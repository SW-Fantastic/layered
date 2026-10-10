#include "../../include/jni/jni.h"
#include <iostream>

#ifdef _WIN32
std::unique_ptr<wchar_t[]> toPlatformStr(const char* str) {

	int size_needed = MultiByteToWideChar(CP_UTF8, 0, str, -1, nullptr, 0);
	if (size_needed <= 0) {
		return nullptr;
	}

	std::unique_ptr<wchar_t[]> result(new wchar_t[size_needed]);
	MultiByteToWideChar(CP_UTF8, 0, str, -1, result.get(), size_needed);
	return result;

}
#endif