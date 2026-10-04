#include "../../include/jni/ClangLibTools.h"
#include "../../include/HeaderAnalysiser.h"

const char** loadStringArray(JNIEnv* env, jobjectArray arr, bool& hasNullPtr) {
	int countParam = env->GetArrayLength(arr);
	const char** csParams = new const char* [countParam];
	for (int idx = 0; idx < countParam; idx++) {
		jstring paramItem = (jstring)env->GetObjectArrayElement(arr, idx);
		if (paramItem) {
			const char* item = env->GetStringUTFChars(paramItem, 0);
			csParams[idx] = item;
		} else {
			csParams[idx] = nullptr;
			hasNullPtr = true;
		}
	}
	return csParams;
}

void releaseStringArray(JNIEnv* env, jobjectArray arr, const char** buf) {
	int countParam = env->GetArrayLength(arr);
	const char** csParams = new const char* [countParam];
	for (int idx = 0; idx < countParam; idx++) {
		const char* strBuf = buf[idx];
		jstring paramItem = (jstring)env->GetObjectArrayElement(arr, idx);
		if (strBuf) {
			env->ReleaseStringUTFChars(paramItem,strBuf);
		}
	}
	delete[] buf;
}

JNIEXPORT jstring JNICALL Java_org_swdc_clang_libtooling_ClangLibTools_parseHeaderFiles
(JNIEnv* env, jclass, jstring workingDir, jobjectArray parameters, jobjectArray paths) {

	if (!workingDir || !parameters || !paths) {
		return env->NewStringUTF("{}");
	}

	int countParam = env->GetArrayLength(parameters);
	int countPaths = env->GetArrayLength(paths);
	if (countPaths == 0) {
		return env->NewStringUTF("{}");
	}

	bool failed = false;

	const char*  csWorkingDir = env->GetStringUTFChars(workingDir, 0);
	const char** csParams = loadStringArray(env, parameters, failed);
	const char** csPaths = loadStringArray(env, paths, failed);

	std::string result = "";
	if (!failed) {
		result = AnalysiserHelper::parseHeader(csWorkingDir, countParam, csParams, countPaths, csPaths);
	}

	releaseStringArray(env, parameters, csParams);
	releaseStringArray(env, paths, csPaths);
	env->ReleaseStringUTFChars(workingDir, csWorkingDir);

	return env->NewStringUTF(result.c_str());
}

