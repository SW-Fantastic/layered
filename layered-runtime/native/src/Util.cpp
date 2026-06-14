#include "../include/jni/jni.h"

void* asWString(const char* javaStr) {

#ifdef _WIN32
	
	int length = MultiByteToWideChar(CP_UTF8, 0, javaStr, -1, NULL, 0);
	if (length <= 0) {
		return NULL;
	}
	wchar_t* result = new wchar_t[length];
	int state = MultiByteToWideChar(CP_UTF8, 0, javaStr, -1, result, length);
	if (state == 0) {
		delete[] result;
		return NULL;
	}
	return result;
#else
	return nullptr;
#endif

}

