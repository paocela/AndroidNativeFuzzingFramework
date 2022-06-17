#include <iostream>
#include <string>
#include <sys/wait.h>
#include <unistd.h>
#include <stdio.h>
#include <fstream>

#include <dlfcn.h>
#include <jni.h>
#include <filesystem>

extern "C"
{
	// types needed by harness
    typedef int JNI_CreateJavaVM_t(void *, void *, void *);
    typedef jint registerNatives_t(JNIEnv *, jclass);
    typedef jint JNI_OnLoad_t(JavaVM *, void *);

	// empty AddSpecialSignalHandlerFn function is necessary
	// It is something libsigchain appears to require. If the function isn’t present it aborts the process
    __attribute__((visibility("default"))) void AddSpecialSignalHandlerFn() 
    { }
}

static auto load_art() -> std::pair<JavaVM *, JNIEnv *>;

// fake functions definitions for JavaVM
jint JNICALL AttachCurrentThread_fake(JavaVM *, JNIEnv **, void *);
jint JNICALL DestroyJavaVM_fake(JavaVM *);
jint JNICALL DetachCurrentThread_fake(JavaVM *);
jint JNICALL AttachCurrentThreadAsDaemon_fake(JavaVM *, JNIEnv **, void *);
jint JNICALL GetEnv_fake(JavaVM *, void **, jint);

// fake functions definitions for JNIEnv
jint JNICALL RegisterNatives_fake(JNIEnv *, jclass , const JNINativeMethod *, jint);
jint JNICALL GetVersion_fake(JNIEnv *);
jclass JNICALL DefineClass_fake(JNIEnv*, const char*, jobject, const jbyte*, jsize);
jclass JNICALL FindClass_fake(JNIEnv *, const char *);
jmethodID JNICALL FromReflectedMethod_fake(JNIEnv*, jobject);
jfieldID JNICALL FromReflectedField_fake(JNIEnv*, jobject);
jobject JNICALL ToReflectedMethod_fake(JNIEnv*, jclass, jmethodID, jboolean);
jclass JNICALL GetSuperclass_fake(JNIEnv*, jclass);
jboolean JNICALL IsAssignableFrom_fake(JNIEnv*, jclass, jclass);
jobject JNICALL ToReflectedField_fake(JNIEnv*, jclass, jfieldID, jboolean);
jint JNICALL Throw_fake(JNIEnv*, jthrowable);
jint JNICALL ThrowNew_fake(JNIEnv *, jclass, const char *);
jthrowable JNICALL ExceptionOccurred_fake(JNIEnv*);
void JNICALL ExceptionDescribe_fake(JNIEnv*);
void JNICALL ExceptionClear_fake(JNIEnv*);
void JNICALL FatalError_fake(JNIEnv*, const char*);
jint JNICALL PushLocalFrame_fake(JNIEnv*, jint);
jobject JNICALL PopLocalFrame_fake(JNIEnv*, jobject);
jobject JNICALL NewGlobalRef_fake(JNIEnv*, jobject);
void JNICALL DeleteGlobalRef_fake(JNIEnv*, jobject);
void JNICALL DeleteLocalRef_fake(JNIEnv*, jobject);
jboolean JNICALL IsSameObject_fake(JNIEnv*, jobject, jobject);
jobject JNICALL NewLocalRef_fake(JNIEnv*, jobject);
jint JNICALL EnsureLocalCapacity_fake(JNIEnv*, jint);
jobject JNICALL AllocObject_fake(JNIEnv*, jclass);
jobject JNICALL NewObject_fake(JNIEnv*, jclass, jmethodID, ...);
jobject JNICALL NewObjectV_fake(JNIEnv*, jclass, jmethodID, va_list);
jobject JNICALL NewObjectA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jclass JNICALL GetObjectClass_fake(JNIEnv*, jobject);
jboolean JNICALL IsInstanceOf_fake(JNIEnv*, jobject, jclass);
jmethodID JNICALL GetMethodID_fake(JNIEnv*, jclass, const char*, const char*);
jobject JNICALL CallObjectMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jobject JNICALL CallObjectMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jobject JNICALL CallObjectMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jboolean JNICALL CallBooleanMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jboolean JNICALL CallBooleanMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jboolean JNICALL CallBooleanMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jbyte JNICALL CallByteMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jbyte JNICALL CallByteMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jbyte JNICALL CallByteMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jchar JNICALL CallCharMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jchar JNICALL CallCharMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jchar JNICALL CallCharMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jshort JNICALL CallShortMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jshort JNICALL CallShortMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jshort JNICALL CallShortMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jint JNICALL CallIntMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jint JNICALL CallIntMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jint JNICALL CallIntMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jlong JNICALL CallLongMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jlong JNICALL CallLongMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jlong JNICALL CallLongMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jfloat JNICALL CallFloatMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jfloat JNICALL CallFloatMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jfloat JNICALL CallFloatMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jdouble JNICALL CallDoubleMethod_fake(JNIEnv*, jobject, jmethodID, ...);
jdouble JNICALL CallDoubleMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
jdouble JNICALL CallDoubleMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
void JNICALL CallVoidMethod_fake(JNIEnv*, jobject, jmethodID, ...);
void JNICALL CallVoidMethodV_fake(JNIEnv*, jobject, jmethodID, va_list);
void JNICALL CallVoidMethodA_fake(JNIEnv*, jobject, jmethodID, const jvalue*);
jobject JNICALL CallNonvirtualObjectMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jobject JNICALL CallNonvirtualObjectMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jobject JNICALL CallNonvirtualObjectMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jboolean JNICALL CallNonvirtualBooleanMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jboolean JNICALL CallNonvirtualBooleanMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jboolean JNICALL CallNonvirtualBooleanMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jbyte JNICALL CallNonvirtualByteMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jbyte JNICALL CallNonvirtualByteMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jbyte JNICALL CallNonvirtualByteMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jchar JNICALL CallNonvirtualCharMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jchar JNICALL CallNonvirtualCharMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jchar JNICALL CallNonvirtualCharMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jshort JNICALL CallNonvirtualShortMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jshort JNICALL CallNonvirtualShortMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jshort JNICALL CallNonvirtualShortMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jint JNICALL CallNonvirtualIntMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jint JNICALL CallNonvirtualIntMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jint JNICALL CallNonvirtualIntMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jlong JNICALL CallNonvirtualLongMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jlong JNICALL CallNonvirtualLongMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jlong JNICALL CallNonvirtualLongMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jfloat JNICALL CallNonvirtualFloatMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jfloat JNICALL CallNonvirtualFloatMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jfloat JNICALL CallNonvirtualFloatMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jdouble JNICALL CallNonvirtualDoubleMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
jdouble JNICALL CallNonvirtualDoubleMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
jdouble JNICALL CallNonvirtualDoubleMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
void JNICALL CallNonvirtualVoidMethod_fake(JNIEnv*, jobject, jclass, jmethodID, ...);
void JNICALL CallNonvirtualVoidMethodV_fake(JNIEnv*, jobject, jclass, jmethodID, va_list);
void JNICALL CallNonvirtualVoidMethodA_fake(JNIEnv*, jobject, jclass, jmethodID, const jvalue*);
jfieldID JNICALL GetFieldID_fake(JNIEnv*, jclass, const char*, const char*);
jobject JNICALL GetObjectField_fake(JNIEnv*, jobject, jfieldID);
jboolean JNICALL GetBooleanField_fake(JNIEnv*, jobject, jfieldID);
jbyte JNICALL GetByteField_fake(JNIEnv*, jobject, jfieldID);
jchar JNICALL GetCharField_fake(JNIEnv*, jobject, jfieldID);
jshort JNICALL GetShortField_fake(JNIEnv*, jobject, jfieldID);
jint JNICALL GetIntField_fake(JNIEnv*, jobject, jfieldID);
jlong JNICALL GetLongField_fake(JNIEnv*, jobject, jfieldID);
jfloat JNICALL GetFloatField_fake(JNIEnv*, jobject, jfieldID);
jdouble JNICALL GetDoubleField_fake(JNIEnv*, jobject, jfieldID);
void JNICALL SetObjectField_fake(JNIEnv*, jobject, jfieldID, jobject);
void JNICALL SetBooleanField_fake(JNIEnv*, jobject, jfieldID, jboolean);
void JNICALL SetByteField_fake(JNIEnv*, jobject, jfieldID, jbyte);
void JNICALL SetCharField_fake(JNIEnv*, jobject, jfieldID, jchar);
void JNICALL SetShortField_fake(JNIEnv*, jobject, jfieldID, jshort);
void JNICALL SetIntField_fake(JNIEnv*, jobject, jfieldID, jint);
void JNICALL SetLongField_fake(JNIEnv*, jobject, jfieldID, jlong);
void JNICALL SetFloatField_fake(JNIEnv*, jobject, jfieldID, jfloat);
void JNICALL SetDoubleField_fake(JNIEnv*, jobject, jfieldID, jdouble);
jmethodID JNICALL GetStaticMethodID_fake(JNIEnv*, jclass, const char*, const char*);
jobject JNICALL CallStaticObjectMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jobject JNICALL CallStaticObjectMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jobject JNICALL CallStaticObjectMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jboolean JNICALL CallStaticBooleanMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jboolean JNICALL CallStaticBooleanMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jboolean JNICALL CallStaticBooleanMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jbyte JNICALL CallStaticByteMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jbyte JNICALL CallStaticByteMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jbyte JNICALL CallStaticByteMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jchar JNICALL CallStaticCharMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jchar JNICALL CallStaticCharMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jchar JNICALL CallStaticCharMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jshort JNICALL CallStaticShortMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jshort JNICALL CallStaticShortMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jshort JNICALL CallStaticShortMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jint JNICALL CallStaticIntMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jint JNICALL CallStaticIntMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jint JNICALL CallStaticIntMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jlong JNICALL CallStaticLongMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jlong JNICALL CallStaticLongMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jlong JNICALL CallStaticLongMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jfloat JNICALL CallStaticFloatMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jfloat JNICALL CallStaticFloatMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jfloat JNICALL CallStaticFloatMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jdouble JNICALL CallStaticDoubleMethod_fake(JNIEnv*, jclass, jmethodID, ...);
jdouble JNICALL CallStaticDoubleMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
jdouble JNICALL CallStaticDoubleMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
void JNICALL CallStaticVoidMethod_fake(JNIEnv*, jclass, jmethodID, ...);
void JNICALL CallStaticVoidMethodV_fake(JNIEnv*, jclass, jmethodID, va_list);
void JNICALL CallStaticVoidMethodA_fake(JNIEnv*, jclass, jmethodID, const jvalue*);
jfieldID JNICALL GetStaticFieldID_fake(JNIEnv*, jclass, const char*, const char*);
jobject JNICALL GetStaticObjectField_fake(JNIEnv*, jclass, jfieldID);
jboolean JNICALL GetStaticBooleanField_fake(JNIEnv*, jclass, jfieldID);
jbyte JNICALL GetStaticByteField_fake(JNIEnv*, jclass, jfieldID);
jchar JNICALL GetStaticCharField_fake(JNIEnv*, jclass, jfieldID);
jshort JNICALL GetStaticShortField_fake(JNIEnv*, jclass, jfieldID);
jint JNICALL GetStaticIntField_fake(JNIEnv*, jclass, jfieldID);
jlong JNICALL GetStaticLongField_fake(JNIEnv*, jclass, jfieldID);
jfloat JNICALL GetStaticFloatField_fake(JNIEnv*, jclass, jfieldID);
jdouble JNICALL GetStaticDoubleField_fake(JNIEnv*, jclass, jfieldID);
void JNICALL SetStaticObjectField_fake(JNIEnv*, jclass, jfieldID, jobject);
void JNICALL SetStaticBooleanField_fake(JNIEnv*, jclass, jfieldID, jboolean);
void JNICALL SetStaticByteField_fake(JNIEnv*, jclass, jfieldID, jbyte);
void JNICALL SetStaticCharField_fake(JNIEnv*, jclass, jfieldID, jchar);
void JNICALL SetStaticShortField_fake(JNIEnv*, jclass, jfieldID, jshort);
void JNICALL SetStaticIntField_fake(JNIEnv*, jclass, jfieldID, jint);
void JNICALL SetStaticLongField_fake(JNIEnv*, jclass, jfieldID, jlong);
void JNICALL SetStaticFloatField_fake(JNIEnv*, jclass, jfieldID, jfloat);
void JNICALL SetStaticDoubleField_fake(JNIEnv*, jclass, jfieldID, jdouble);
jstring JNICALL NewString_fake(JNIEnv*, const jchar*, jsize);
jsize JNICALL GetStringLength_fake(JNIEnv*, jstring);
const jchar * JNICALL GetStringChars_fake(JNIEnv*, jstring, jboolean*);
void JNICALL ReleaseStringChars_fake(JNIEnv*, jstring, const jchar*);
jstring JNICALL NewStringUTF_fake(JNIEnv*, const char*);
jsize JNICALL GetStringUTFLength_fake(JNIEnv*, jstring);
const char * JNICALL GetStringUTFChars_fake(JNIEnv*, jstring, jboolean*);
void JNICALL ReleaseStringUTFChars_fake(JNIEnv*, jstring, const char*);
jsize JNICALL GetArrayLength_fake(JNIEnv*, jarray);
jobjectArray JNICALL NewObjectArray_fake(JNIEnv*, jsize, jclass, jobject);
jobject JNICALL GetObjectArrayElement_fake(JNIEnv*, jobjectArray, jsize);
void JNICALL SetObjectArrayElement_fake(JNIEnv*, jobjectArray, jsize, jobject);
jbooleanArray JNICALL NewBooleanArray_fake(JNIEnv*, jsize);
jbyteArray JNICALL NewByteArray_fake(JNIEnv*, jsize);
jcharArray JNICALL NewCharArray_fake(JNIEnv*, jsize);
jshortArray JNICALL NewShortArray_fake(JNIEnv*, jsize);
jintArray JNICALL NewIntArray_fake(JNIEnv*, jsize);
jlongArray JNICALL NewLongArray_fake(JNIEnv*, jsize);
jfloatArray JNICALL NewFloatArray_fake(JNIEnv*, jsize);
jdoubleArray JNICALL NewDoubleArray_fake(JNIEnv*, jsize);
jboolean* JNICALL GetBooleanArrayElements_fake(JNIEnv*, jbooleanArray, jboolean*);
jbyte* JNICALL GetByteArrayElements_fake(JNIEnv*, jbyteArray, jboolean*);
jchar* JNICALL GetCharArrayElements_fake(JNIEnv*, jcharArray, jboolean*);
jshort* JNICALL GetShortArrayElements_fake(JNIEnv*, jshortArray, jboolean*);
jint* JNICALL GetIntArrayElements_fake(JNIEnv*, jintArray, jboolean*);
jlong* JNICALL GetLongArrayElements_fake(JNIEnv*, jlongArray, jboolean*);
jfloat* JNICALL GetFloatArrayElements_fake(JNIEnv*, jfloatArray, jboolean*);
jdouble* JNICALL GetDoubleArrayElements_fake(JNIEnv*, jdoubleArray, jboolean*);
void JNICALL ReleaseBooleanArrayElements_fake(JNIEnv*, jbooleanArray, jboolean*, jint);
void JNICALL ReleaseByteArrayElements_fake(JNIEnv*, jbyteArray, jbyte*, jint);
void JNICALL ReleaseCharArrayElements_fake(JNIEnv*, jcharArray, jchar*, jint);
void JNICALL ReleaseShortArrayElements_fake(JNIEnv*, jshortArray, jshort*, jint);
void JNICALL ReleaseIntArrayElements_fake(JNIEnv*, jintArray, jint*, jint);
void JNICALL ReleaseLongArrayElements_fake(JNIEnv*, jlongArray, jlong*, jint);
void JNICALL ReleaseFloatArrayElements_fake(JNIEnv*, jfloatArray, jfloat*, jint);
void JNICALL ReleaseDoubleArrayElements_fake(JNIEnv*, jdoubleArray, jdouble*, jint);
void JNICALL GetBooleanArrayRegion_fake(JNIEnv*, jbooleanArray, jsize, jsize, jboolean*);
void JNICALL GetByteArrayRegion_fake(JNIEnv*, jbyteArray, jsize, jsize, jbyte*);
void JNICALL GetCharArrayRegion_fake(JNIEnv*, jcharArray, jsize, jsize, jchar*);
void JNICALL GetShortArrayRegion_fake(JNIEnv*, jshortArray, jsize, jsize, jshort*);
void JNICALL GetIntArrayRegion_fake(JNIEnv*, jintArray, jsize, jsize, jint*);
void JNICALL GetLongArrayRegion_fake(JNIEnv*, jlongArray, jsize, jsize, jlong*);
void JNICALL GetFloatArrayRegion_fake(JNIEnv*, jfloatArray, jsize, jsize, jfloat*);
void JNICALL GetDoubleArrayRegion_fake(JNIEnv*, jdoubleArray, jsize, jsize, jdouble*);
void JNICALL SetBooleanArrayRegion_fake(JNIEnv*, jbooleanArray, jsize, jsize, const jboolean*);
void JNICALL SetByteArrayRegion_fake(JNIEnv*, jbyteArray, jsize, jsize, const jbyte*);
void JNICALL SetCharArrayRegion_fake(JNIEnv*, jcharArray,jsize, jsize, const jchar*);
void JNICALL SetShortArrayRegion_fake(JNIEnv*, jshortArray, jsize, jsize, const jshort*);
void JNICALL SetIntArrayRegion_fake(JNIEnv*, jintArray, jsize, jsize, const jint*);
void JNICALL SetLongArrayRegion_fake(JNIEnv*, jlongArray, jsize, jsize, const jlong*);
void JNICALL SetFloatArrayRegion_fake(JNIEnv*, jfloatArray, jsize, jsize, const jfloat*);
void JNICALL SetDoubleArrayRegion_fake(JNIEnv*, jdoubleArray, jsize, jsize, const jdouble*);
jint JNICALL RegisterNatives_fake(JNIEnv*, jclass, const JNINativeMethod*, jint);
jint JNICALL UnregisterNatives_fake(JNIEnv*, jclass);
jint JNICALL MonitorEnter_fake(JNIEnv*, jobject);
jint JNICALL MonitorExit_fake(JNIEnv*, jobject);
jint JNICALL GetJavaVM_fake(JNIEnv*, JavaVM**);
void JNICALL GetStringRegion_fake(JNIEnv*, jstring, jsize, jsize, jchar*);
void JNICALL GetStringUTFRegion_fake(JNIEnv*, jstring, jsize, jsize, char*);
void* JNICALL GetPrimitiveArrayCritical_fake(JNIEnv*, jarray, jboolean*);
void JNICALL ReleasePrimitiveArrayCritical_fake(JNIEnv*, jarray, void*, jint);
const jchar * JNICALL GetStringCritical_fake(JNIEnv*, jstring, jboolean*);
void JNICALL ReleaseStringCritical_fake(JNIEnv*, jstring, const jchar*);
jweak JNICALL NewWeakGlobalRef_fake(JNIEnv*, jobject);
void JNICALL DeleteWeakGlobalRef_fake(JNIEnv*, jweak);
jboolean JNICALL ExceptionCheck_fake(JNIEnv*);
jobject JNICALL NewDirectByteBuffer_fake(JNIEnv*, void*, jlong);
void* JNICALL GetDirectBufferAddress_fake(JNIEnv*, jobject);
jlong JNICALL GetDirectBufferCapacity_fake(JNIEnv*, jobject);
jobjectRefType JNICALL GetObjectRefType_fake(JNIEnv*, jobject);

// find function pointer definition
int findFunctionPtrSharedLib();
