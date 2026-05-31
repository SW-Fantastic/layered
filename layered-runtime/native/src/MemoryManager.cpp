#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "../include/MemoryManager.h"

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    sizeOfPointer
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_sizeOfPointer
(JNIEnv*, jclass) {
	return sizeof(intptr_t);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    sizeOfInt
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_sizeOfInt
(JNIEnv*, jclass) {
	return sizeof(int32_t);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    sizeOfBoolean
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_sizeOfBoolean
(JNIEnv*, jclass) {
	return sizeof(bool);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    sizeOfLong
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_sizeOfLong
(JNIEnv*, jclass) {
	return sizeof(long);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    sizeOfDouble
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_sizeOfDouble
(JNIEnv*, jclass) {
	return sizeof(double);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    sizeOfFloat
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_sizeOfFloat
(JNIEnv*, jclass) {
	return sizeof(float);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    sizeOfByte
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_sizeOfByte
(JNIEnv*, jclass) {
	return sizeof(uint8_t);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    sizeOfShort
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_sizeOfShort
(JNIEnv*, jclass) {
	return sizeof(short);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    malloc
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_MemoryManager_malloc
(JNIEnv* env, jclass clazz, jint size) {

	if (size == 0 || size < 0) {
		return 0;
	}

	void* addr = je_malloc(size);
	if (addr == NULL) {
		return 0;
	}
	return reinterpret_cast<jlong>(addr);

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    mallocAligned
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_MemoryManager_mallocAligned
(JNIEnv*, jclass, jint size, jint alignment) {

	if (size <= 0 || alignment % 2 != 0) {
		return 0L;
	}

	void* addr = je_aligned_alloc(alignment, size);
	return reinterpret_cast<intptr_t>(addr);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    free
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_free
(JNIEnv* env, jclass clazz, jlong addr) {

	if (addr == NULL) {
		return 0;
	}

	void* pAddr = reinterpret_cast<void*>(addr);
	je_free(pAddr);
	return 1;
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    freeAligned
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_freeAligned
(JNIEnv*, jclass, jlong addr) {

	if (addr == NULL) {
		return 0;
	}
	void* target = reinterpret_cast<void*>(addr);
	je_free(target);
	return 1;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    memset
 * Signature: (JIJ)Z
 */
JNIEXPORT jboolean JNICALL Java_org_swdc_layered_MemoryManager_memset
(JNIEnv* env, jclass clazz, jlong addr, jint value, jlong size) {

	if (addr == NULL || size <= 0) {
		return JNI_FALSE;
	}

	void* pAddr = reinterpret_cast<void*>(addr);
	memset(pAddr, value, size);
	return JNI_TRUE;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    memcpy
 * Signature: (JJJ)Z
 */
JNIEXPORT jboolean JNICALL Java_org_swdc_layered_MemoryManager_memcpy
(JNIEnv*, jclass, jlong addrDst, jlong addrSrc, jlong size) {

	if (addrDst == NULL || addrSrc == NULL || size <= 0) {
		return JNI_FALSE;
	}

	void* pDst = reinterpret_cast<void*>(addrDst);
	void* pSrc = reinterpret_cast<void*>(addrSrc);
	memcpy(pDst, pSrc, size);

	return JNI_TRUE;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    strlen
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_MemoryManager_strlen
(JNIEnv*, jclass, jlong address) {
	
	if (address == NULL) {
		return 0;
	}
	char* str = reinterpret_cast<char*>(address);
	return strlen(str);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    directBuffer
 * Signature: (JJ)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_swdc_layered_MemoryManager_directBuffer
(JNIEnv* env, jclass, jlong addr, jlong size) {

	if (addr == NULL) {
		return NULL;
	}

	void* ptr = reinterpret_cast<void*>(addr);
	return env->NewDirectByteBuffer(ptr, size);
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readInt
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_org_swdc_layered_MemoryManager_readInt
(JNIEnv*, jclass, jlong addr, jint index) {

	if (addr == NULL || index < 0) {
		return 0;
	}

	int * pAddr = reinterpret_cast<int*>(addr);
	int   value = pAddr[index];

	return value;
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readIntArray
 * Signature: (JI)[I
 */
JNIEXPORT jintArray JNICALL Java_org_swdc_layered_MemoryManager_readIntArray
(JNIEnv* env, jclass, jlong addr, jint size) {

	if (addr == NULL || size <= 0) {
		return NULL;
	}

	int* pBuf = reinterpret_cast<int*>(addr);
	jintArray array = env->NewIntArray(size);
	env->SetIntArrayRegion(array, 0, size, (jint*)pBuf);
	return array;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeIntArray
 * Signature: (J[I)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeIntArray
(JNIEnv* env, jclass clazz, jlong addr, jintArray src) {

	if (addr == NULL || src == NULL) {
		return;
	}

	int* pBuf = reinterpret_cast<int*>(addr);
	jboolean copyFlg = 0;
	jint* array = env->GetIntArrayElements(src, &copyFlg);
	memcpy(pBuf, array, env->GetArrayLength(src) * sizeof(int));
	env->ReleaseIntArrayElements(src, array, 0);

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeInt
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeInt
(JNIEnv*, jclass, jlong addr, jint index, jint value) {

	if (addr == NULL || index < 0) {
		return;
	}

	int* pBuf = reinterpret_cast<int*>(addr);
	pBuf[index] = value;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readLong
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_MemoryManager_readLong
(JNIEnv*, jclass, jlong addr, jint index) {

	if (addr == NULL || index < 0) {
		return 0;
	}

	long* buf = reinterpret_cast<long*>(addr);
	return buf[index];

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readLongArray
 * Signature: (JI)[J
 */
JNIEXPORT jlongArray JNICALL Java_org_swdc_layered_MemoryManager_readLongArray
(JNIEnv* env, jclass, jlong addr, jint size) {

	if (addr == NULL || size <= 0) {
		return NULL;
	}

	long* pBuf = reinterpret_cast<long*>(addr);
	jlongArray array = env->NewLongArray(size);

#ifdef _WIN32
	// long in windows platform is 32bit (same as int)
	int64_t* buf_t = new int64_t[size];
	for (int index = 0; index < size; index++) {
		buf_t[index] = pBuf[index];
	}
	env->SetLongArrayRegion(array, 0, size, (jlong*)buf_t);
	delete[] buf_t;
#else
	// long is 64bit in other platforms
	env->SetLongArrayRegion(array, 0, size, (jlong*)pBuf);
#endif

	return array;
}


/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeLong
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeLong
(JNIEnv*, jclass, jlong addr, jint index, jlong value) {

	if (addr == NULL || index < 0) {
		return;
	}
	long* buf = reinterpret_cast<long*>(addr);
	buf[index] = value;

}


/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeLongArray
 * Signature: (J[J)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeLongArray
(JNIEnv* env, jclass, jlong addr, jlongArray src) {

	if (addr == NULL || src == NULL) {
		return;
	}
	
	long* pBuf = reinterpret_cast<long*>(addr);
	jboolean copyFlg = 0;
	jlong* srcBuf = env->GetLongArrayElements(src, &copyFlg);
#ifdef _WIN32
	// long in windows platform is 32bit (same as int)
	jint size = env->GetArrayLength(src);
	for (int index = 0; index < size; index++) {
		pBuf[index] = static_cast<long>(srcBuf[index]);
	}
#else
	// long is 64bit in other platforms
	memcpy(pBuf, srcBuf, sizeof(long) * env->GetArrayLength(src));
#endif
	env->ReleaseLongArrayElements(src, srcBuf, 0);

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readDouble
 * Signature: (JI)D
 */
JNIEXPORT jdouble JNICALL Java_org_swdc_layered_MemoryManager_readDouble
(JNIEnv*, jclass, jlong addr, jint index) {

	if (addr == NULL || index < 0) {
		return 0;
	}

	double* pBuf = reinterpret_cast<double*>(addr);
	return pBuf[index];

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readDoubleArray
 * Signature: (JI)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_org_swdc_layered_MemoryManager_readDoubleArray
(JNIEnv* env, jclass, jlong addr, jint size) {

	if (size <= 0 || addr == NULL) {
		return NULL;
	}

	double* pBuf = reinterpret_cast<double*>(addr);
	jdoubleArray arr = env->NewDoubleArray(size);
	env->SetDoubleArrayRegion(arr, 0, size, (jdouble*)pBuf);
	return arr;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeDouble
 * Signature: (JID)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeDouble
(JNIEnv*, jclass, jlong addr, jint index, jdouble value) {

	if (addr == NULL || index < 0) {
		return;
	}

	double* pBuf = reinterpret_cast<double*>(addr);
	pBuf[index] = value;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeDoubleArray
 * Signature: (J[D)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeDoubleArray
(JNIEnv* env, jclass, jlong addr, jdoubleArray src) {

	if (addr == NULL || src == NULL) {
		return;
	}


	int      length   = env->GetArrayLength(src);
	double * pBuf     = reinterpret_cast<double*>(addr);
	jboolean copyFlg  = 0;
	jdouble* elements = env->GetDoubleArrayElements(src, &copyFlg);
	
	memcpy(pBuf, elements, length * sizeof(double));
	env->ReleaseDoubleArrayElements(src, elements, 0);

}


/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readFloat
 * Signature: (JI)F
 */
JNIEXPORT jfloat JNICALL Java_org_swdc_layered_MemoryManager_readFloat
(JNIEnv*, jclass, jlong addr, jint index) {

	if (addr == NULL || index < 0) {
		return 0;
	}

	float* pBuf = reinterpret_cast<float*>(addr);
	return pBuf[index];

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readFloatArray
 * Signature: (JI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_org_swdc_layered_MemoryManager_readFloatArray
(JNIEnv* env, jclass, jlong addr, jint size) {

	if (addr == NULL || size <= 0) {
		return NULL;
	}

	float *     pBuf  = reinterpret_cast<float*>(addr);
	jfloatArray array = env->NewFloatArray(size);
	
	env->SetFloatArrayRegion(array, 0, size, (jfloat*)pBuf);
	return array;
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeFloat
 * Signature: (JIF)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeFloat
(JNIEnv*, jclass, jlong addr, jint index, jfloat value) {

	if (addr == NULL || index < 0) {
		return;
	}

	float* pBuf = reinterpret_cast<float*>(addr);
	pBuf[index] = value;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeFloatArray
 * Signature: (J[F)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeFloatArray
(JNIEnv* env, jclass, jlong addr, jfloatArray src) {

	if (addr == NULL || src == NULL) {
		return;
	}

	float* pBuf = reinterpret_cast<float*>(addr);
	jboolean copyFlg = 0;
	jfloat*  source = env->GetFloatArrayElements(src, &copyFlg);
	memcpy(pBuf, source, env->GetArrayLength(src) * sizeof(float));
	env->ReleaseFloatArrayElements(src, source, 0);

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readShort
 * Signature: (JI)S
 */
JNIEXPORT jshort JNICALL Java_org_swdc_layered_MemoryManager_readShort
(JNIEnv*, jclass, jlong addr, jint index) {

	if (addr == NULL || index < 0) {
		return 0;
	}

	short* pBuf = reinterpret_cast<short*>(addr);
	return pBuf[index];

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readShortArray
 * Signature: (JI)[S
 */
JNIEXPORT jshortArray JNICALL Java_org_swdc_layered_MemoryManager_readShortArray
(JNIEnv* env, jclass, jlong addr, jint size) {
	
	if (addr == NULL || size < 0) {
		return 0;
	}

	jshortArray array = env->NewShortArray(size);
	short* pBuf = reinterpret_cast<short*>(addr);
	env->SetShortArrayRegion(array, 0, size, (jshort*)pBuf);
	return array;
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeShort
 * Signature: (JIS)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeShort
(JNIEnv*, jclass, jlong addr, jint index, jshort value) {

	if (addr == NULL || index < 0) {
		return;
	}
	short* pBuf = reinterpret_cast<short*>(addr);
	pBuf[index] = value;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeShortArray
 * Signature: (J[S)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeShortArray
(JNIEnv* env, jclass, jlong addr, jshortArray src) {

	if (addr == NULL || src == NULL) {
		return;
	}

	short  *  pBuf    = reinterpret_cast<short*>(addr);
	int       size    = env->GetArrayLength(src);
	jboolean  copyFlg = 0;
	jshort *  pSrc = env->GetShortArrayElements(src, &copyFlg);

	memcpy(pBuf, pSrc, sizeof(short) * size);
	env->ReleaseShortArrayElements(src, pSrc, 0);

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readAddress
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_org_swdc_layered_MemoryManager_readAddress
(JNIEnv*, jclass, jlong addr, jint index) {

	if (addr == NULL || index < 0) {
		return 0;
	}

	intptr_t * pBuf = reinterpret_cast<intptr_t*>(addr);
	return pBuf[index];
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readAddressArray
 * Signature: (JI)[J
 */
JNIEXPORT jlongArray JNICALL Java_org_swdc_layered_MemoryManager_readAddressArray
(JNIEnv* env, jclass, jlong addr, jint size) {

	if (addr == NULL || size <= 0) {
		return NULL;
	}

	intptr_t* pbuf = reinterpret_cast<intptr_t*>(addr);
	jlongArray array = env->NewLongArray(size);
	env->SetLongArrayRegion(array, 0, size, pbuf);
	return array;
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeAddress
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeAddress
(JNIEnv*, jclass, jlong addr, jint index, jlong value) {

	if (addr == NULL || index < 0) {
		return;
	}

	intptr_t* pBuf = reinterpret_cast<intptr_t*>(addr);
	pBuf[index] = static_cast<intptr_t>(value);

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeAddressArray
 * Signature: (J[J)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeAddressArray
(JNIEnv* env, jclass, jlong addr, jlongArray src) {

	if (addr == NULL || src == NULL) {
		return;
	}

	intptr_t  *      pBuf = reinterpret_cast<intptr_t*>(addr);
	int            length = env->GetArrayLength(src);
	jboolean      copyFlg = 0;
	jlong     *    srcBuf = env->GetLongArrayElements(src, &copyFlg);

	memcpy(pBuf, srcBuf, sizeof(intptr_t) * length);
	env->ReleaseLongArrayElements(src, srcBuf, 0);

}



/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readBoolean
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_org_swdc_layered_MemoryManager_readBoolean
(JNIEnv*, jclass, jlong addr, jint index) {

	if (addr == NULL || index < 0) {
		return JNI_FALSE;
	}
	bool* pBuf = reinterpret_cast<bool*>(addr);
	return pBuf[index] ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readBooleanArray
 * Signature: (JI)[Z
 */
JNIEXPORT jbooleanArray JNICALL Java_org_swdc_layered_MemoryManager_readBooleanArray
(JNIEnv* env, jclass, jlong addr, jint size) {

	if (addr == NULL || size <= 0) {
		return NULL;
	}


	bool      *     pBuf  = reinterpret_cast<bool*>(addr);
	jboolean  *     pWrap = new jboolean[size];
	
	for (int index = 0; index < size; index++) {
		pWrap[index] = pBuf[index] ? JNI_TRUE : JNI_FALSE;
	}

	jbooleanArray   array = env->NewBooleanArray(size);
	env->SetBooleanArrayRegion(array, 0, size, pWrap);
	delete[] pWrap;

	return array;
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeBoolean
 * Signature: (JIZ)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeBoolean
(JNIEnv*, jclass, jlong addr, jint index, jboolean value) {

	if (addr == NULL || index < 0) {
		return;
	}

	bool  * pBuf = reinterpret_cast<bool*>(addr);
	pBuf[index] = (value == JNI_TRUE);

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeBooleanArray
 * Signature: (J[Z)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeBooleanArray
(JNIEnv* env, jclass, jlong addr, jbooleanArray src) {

	if (addr == NULL || src == NULL) {
		return;
	}
	
	int           size = env->GetArrayLength(src);
	bool     *    pBuf = reinterpret_cast<bool*>(addr);
	jboolean   copyFlg = 0;
	jboolean*     pSrc = env->GetBooleanArrayElements(src, &copyFlg);
	for (int index = 0; index < size; index++) {
		pBuf[index] = (pSrc[index] == JNI_TRUE);
	}
}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readByte
 * Signature: (JI)B
 */
JNIEXPORT jbyte JNICALL Java_org_swdc_layered_MemoryManager_readByte
(JNIEnv*, jclass, jlong address, jint index) {

	if (address == NULL || index < 0) {
		return 0;
	}

	uint8_t* bytePtr = reinterpret_cast<uint8_t*>(address);
	return bytePtr[index];

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    readByteArray
 * Signature: (JII)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_swdc_layered_MemoryManager_readByteArray
(JNIEnv* env, jclass, jlong addr, jint offset, jint size) {

	if (addr == NULL || size <= 0) {
		return NULL;
	}

	uint8_t* ptr = reinterpret_cast<uint8_t*>(addr);
	uint8_t* targetSrc = ptr + offset;
	
	jbyteArray arr = env->NewByteArray(size);
	env->SetByteArrayRegion(arr, 0, size, (jbyte*)targetSrc);
	return arr;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeByte
 * Signature: (JIB)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeByte
(JNIEnv*, jclass, jlong addr, jint index, jbyte data) {

	if (addr == NULL || index < 0) {
		return;
	}

	uint8_t* bytePtr = reinterpret_cast<uint8_t*>(addr);
	bytePtr[index] = data;

}

/*
 * Class:     org_swdc_layered_MemoryManager
 * Method:    writeByteArray
 * Signature: (JI[BII)V
 */
JNIEXPORT void JNICALL Java_org_swdc_layered_MemoryManager_writeByteArray
(JNIEnv* env, jclass, jlong addr, jint dstOffset, jbyteArray src, jint srcOffset, jint size) {

	if (addr == NULL || src == NULL) {
		return;
	}

	uint8_t* bytePtr = reinterpret_cast<uint8_t*>(addr);
	uint8_t* targetPtr = bytePtr + dstOffset;

	jboolean cpyFlag = 0;
	jbyte* srcPtr = env->GetByteArrayElements(src, &cpyFlag);
	jbyte* srcTargetPtr = srcPtr + srcOffset;
	memcpy(targetPtr, srcTargetPtr, size);
	env->ReleaseByteArrayElements(src,srcPtr,0);

}

