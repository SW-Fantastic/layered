#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "../include/ExternalInvoker.h"

static JavaVM* javaVM;
static jmethodID closureMethod;

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {

	JNIEnv* env = nullptr;
	jint result = -1;

	if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
		return JNI_ERR;
	}

	result = JNI_VERSION_1_6;
	javaVM = vm;

	jclass closureClazz = env->FindClass("org/swdc/layered/def/PlatformClosure");
	if (closureClazz == NULL) {
		return JNI_ERR;
	}
	jmethodID method = env->GetMethodID(closureClazz, "byCall", "(JJ)V");
	if (method == NULL) {
		return JNI_ERR;
	}
	closureMethod = method;

	return result;
}

ffi_type* getByLayerFlag(int layerTypeFlag) {
	switch (layerTypeFlag) {
	case LAYER_ADDRESS:
		return &ffi_type_pointer;
	case LAYER_INT:
		return  &ffi_type_sint;
	case LAYER_UINT:
		return &ffi_type_uint;
	case LAYER_LONG:
		return &ffi_type_slong;
	case LAYER_ULONG:
		return &ffi_type_ulong;
	case LAYER_FLOAT:
		return &ffi_type_float;
	case LAYER_DOUBLE:
		return &ffi_type_double;
	case LAYER_CHAR:
		return &ffi_type_schar;
	case LAYER_UCHAR:
		return &ffi_type_uchar;
	case LAYER_VOID:
		return &ffi_type_void;
	case LAYER_BOOL:
		return &ffi_type_uint;
	}
	return NULL;
}

/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    loadLibrary
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_ExternalInvoker_loadLibrary
(JNIEnv* env, jclass, jstring path) {

	jboolean copyFlg = 0;
	const char* pPath = env->GetStringUTFChars(path, &copyFlg);
	jlong result = 0;

#ifdef _WIN32
	
	HMODULE handle = LoadLibrary(pPath);
	env->ReleaseStringUTFChars(path, pPath);
	return reinterpret_cast<intptr_t>(handle);

#else 

	void* result = dlopen(pPath);
	env->ReleaseStringUTFChars(path, pPath);
	return reinterpret_cast<intptr_t>(result);

#endif

}

/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    unloadLibrary
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_ExternalInvoker_unloadLibrary
(JNIEnv*, jclass, jlong address) {

	if (address == 0) {
		return;
	}

#ifdef _WIN32
	
	HMODULE module = reinterpret_cast<HMODULE>(address);
	FreeLibrary(module);

#else

	intptr_t addr = reinterpret_cast<intptr_t>(address);
	dlclose(addr);

#endif
}

/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    getLibrarySymbols
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_swdc_layered_ExternalInvoker_getLibrarySymbols
(JNIEnv* env, jclass, jlong address) {

	if (address == NULL) {
		return NULL;
	}

#ifdef _WIN32

	HMODULE target = reinterpret_cast<HMODULE>(address);
	void* addr = GetProcAddress(target, "getMetaData");
	if (addr == NULL) {
		return NULL;
	}

	void* addrGetSize = GetProcAddress(target, "getMetaDataSize");
	if (addrGetSize == NULL) {
		return NULL;
	}

	const int (*getMetaDataSize)(void) = reinterpret_cast<const int(*)(void)>(addrGetSize);
	const unsigned char* (*getMetadata)(void) = reinterpret_cast<const unsigned char* (*)(void)>(addr);

	const int size = getMetaDataSize();
	const unsigned char* metadata = getMetadata();
	return env->NewDirectByteBuffer((void*)metadata, size);
#else

	void* target = reinterpret_cast<void*>(address);
	void* addr = dlsym(target, "getMetaData");
	if (addr == NULL) {
		return NULL;
	}

	void* addrGetSize = dlsym(target, "getMetaDataSize");
	if (addrGetSize == NULL) {
		return NULL;
	}

	const int size = getMetaDataSize();
	const char* metadata = getMetadata();
	return env->NewDirectByteBuffer((void*)metadata, size);

#endif

}

/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    lookup
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_ExternalInvoker_lookup
(JNIEnv* env, jclass, jlong modulePtr, jstring symbol) {


	const char* symbolPtr = env->GetStringUTFChars(symbol, 0);
	void* targetFunctionPtr = NULL;
#ifdef _WIN32

	HMODULE target = reinterpret_cast<HMODULE>(modulePtr);
	targetFunctionPtr = GetProcAddress(target, symbolPtr);

#else 

	void* target = reinterpret_cast<void*>(modulePtr);
	targetFunctionPtr = dlsym(target, "getMetaDataSize");

#endif

	env->ReleaseStringUTFChars(symbol, symbolPtr);
	return reinterpret_cast<intptr_t>(targetFunctionPtr);

}


/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    getLastError
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_swdc_layered_ExternalInvoker_getLastError
(JNIEnv* env, jclass) {

#ifdef _WIN32

	DWORD lastErr = GetLastError();
	LPVOID buffer = NULL;
	FormatMessageW(
		FORMAT_MESSAGE_ALLOCATE_BUFFER |
		FORMAT_MESSAGE_FROM_SYSTEM |
		FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL,
		lastErr,
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
		(LPWSTR)&buffer,
		0,
		NULL
	);

	if (buffer != NULL) {
		
		LPWSTR buf = (LPWSTR)buffer;
		int length = wcslen(buf);
		int sizeNeed = WideCharToMultiByte(CP_UTF8, 0, (wchar_t*)buffer, length,NULL, 0,NULL,NULL);
		if (sizeNeed == 0) {
			LocalFree(buffer);
			return NULL;
		}

		char * data = new char[sizeNeed];
		WideCharToMultiByte(CP_UTF8, 0, buf, length, data, sizeNeed, NULL, NULL);

		jstring str = env->NewStringUTF(data);
		LocalFree(buffer);
		delete[] data;

		return str;
	}

	return NULL;
#else

	char* msg = dlerror();
	if (msg != NULL) {
		jstring str = env->NewStringUTF((char*)buffer);
		return str;
	}
	return NULL;

#endif

}

/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    createFFICIF
 * Signature: ([II)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_ExternalInvoker_createFFICIF
(JNIEnv* env, jclass, jintArray argTypeFlags, jint resultTypeFlag) {

	if (argTypeFlags == NULL) {
		return 0;
	}


	ffi_type* resultType = getByLayerFlag(resultTypeFlag);
	if (resultType == NULL) {
		return 0;
	}

	jint* argFlags = env->GetIntArrayElements(argTypeFlags, 0);
	int argSize = env->GetArrayLength(argTypeFlags);
	ffi_type** types = new ffi_type*[argSize];
	ffi_cif* callingHandler = new ffi_cif();

	if (argSize > 0) {
		for (int index = 0; index < argSize; index++) {
			ffi_type* type = getByLayerFlag(argFlags[index]);
			if (type == NULL) {
				env->ReleaseIntArrayElements(argTypeFlags, argFlags, 0);
				delete[] types;
				delete callingHandler;
				return 0;
			}
			else {
				types[index] = type;
			}
		}
	} else {
		types = NULL;
	}
	
	ffi_status state = ffi_prep_cif(callingHandler, FFI_DEFAULT_ABI, argSize, resultType, types);
	if (state == FFI_OK) {
		LayerCall* call = new LayerCall();
		call->types = types;
		call->cif = callingHandler;
		return reinterpret_cast<intptr_t>(call);
	}

	return 0;
}

/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    destroyFFICIF
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_ExternalInvoker_destroyFFICIF
(JNIEnv*, jclass, jlong cifAddr) {

	if (cifAddr == NULL) {
		return;
	}

	LayerCall* call = reinterpret_cast<LayerCall*>(cifAddr);
	ffi_cif* cifPtr = call->cif;
	ffi_type** types = call->types;
	delete[] types;
	delete cifPtr;
	delete call;

}


/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    call
 * Signature: (JJJJ[I)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_ExternalInvoker_call
(JNIEnv* env, jclass, jlong cifAddr, jlong resultAddr, jlong functionAddr, jlong paramsAddr, jintArray layerFlags) {

	if (cifAddr == NULL || functionAddr == NULL || resultAddr == NULL) {
		return;
	}


	int paramSize = env->GetArrayLength(layerFlags);
	jint* argTypes = env->GetIntArrayElements(layerFlags, 0);
	void** realParams = NULL;

	if (paramsAddr != NULL) {
		realParams = new void* [paramSize];
		void** params = reinterpret_cast<void**>(paramsAddr);
		for (int index = 0; index < paramSize; index++) {
			if (argTypes[index] == LAYER_ADDRESS) {
				realParams[index] = &params[index];
			}
			else {
				realParams[index] = params[index];
			}
		}
	}
	
	LayerCall* call = reinterpret_cast<LayerCall*>(cifAddr);
	ffi_cif* cifPtr = call->cif;

	void* functionPtr = reinterpret_cast<void*>(functionAddr);
	void* result = reinterpret_cast<void*>(resultAddr);
	ffi_call(cifPtr, FFI_FN(functionPtr), result, realParams);

	if (realParams != NULL) {
		delete[] realParams;
	}

}

void closureCall(ffi_cif* cif, void* ret, void** args, void* user_data) {

	JNIEnv* env = NULL;
	javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);
	if (env == NULL) {
		javaVM->AttachCurrentThread((void**)&env, NULL);
	}

	env->CallVoidMethod((jobject)user_data, closureMethod, reinterpret_cast<intptr_t>(ret), reinterpret_cast<intptr_t>(args));

}


/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    createClosure
 * Signature: (Ljava/lang/Object;J)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_ExternalInvoker_createClosure
(JNIEnv* env, jclass, jobject closure, jlong cifAddr) {

	void* executeAddr = NULL;
	ffi_closure* theClosure = (ffi_closure*)ffi_closure_alloc(sizeof(ffi_closure), &executeAddr);
	if (executeAddr == NULL || closure == NULL || cifAddr == NULL) {
		return NULL;
	}

	closure = env->NewGlobalRef(closure);
	LayerCall* call = reinterpret_cast<LayerCall*>(cifAddr);

	ffi_prep_closure_loc(theClosure, call->cif, closureCall, closure, executeAddr);

	LayerCallBack* callback = new LayerCallBack();
	callback->closure = theClosure;
	callback->callingAddr = executeAddr;
	callback->javaCb = closure;

	return reinterpret_cast<intptr_t>(callback);

}

/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    getClosureFunctionAddr
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_ExternalInvoker_getClosureFunctionAddr
(JNIEnv*, jclass, jlong closureAddr) {

	if (closureAddr == NULL) {
		return 0L;
	}

	LayerCallBack* callback = reinterpret_cast<LayerCallBack*>(closureAddr);
	return reinterpret_cast<intptr_t>(callback->callingAddr);

}

/*
 * Class:     org_swdc_layered_ExternalInvoker
 * Method:    freeClosure
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_ExternalInvoker_freeClosure
(JNIEnv* env, jclass, jlong closureAddr) {

	if (closureAddr == NULL) {
		return;
	}

	LayerCallBack* callback = reinterpret_cast<LayerCallBack*>(closureAddr);
	ffi_closure* closure = (ffi_closure*)callback->closure;
	jobject javaClosure = (jobject)callback->javaCb;
	env->DeleteGlobalRef(javaClosure);
	ffi_closure_free(closure);

}