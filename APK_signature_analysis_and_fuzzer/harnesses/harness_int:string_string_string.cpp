#include <iostream>
#include <string>
#include <sys/wait.h>
#include <unistd.h>
#include <stdio.h>
#include <fstream>


#if defined(__ANDROID__)

#include <dlfcn.h>
#include <jni.h>
#include <filesystem>

namespace fs = std::filesystem;

extern "C"
{
    typedef int JNI_CreateJavaVM_t(void *, void *, void *);
    typedef jint registerNatives_t(JNIEnv *, jclass);
    typedef jint JNI_OnLoad_t(JavaVM *, void *);

    typedef jint function_t(JNIEnv *, jclass __unused, jstring, jstring, jstring, jstring);

    __attribute__((visibility("default"))) void AddSpecialSignalHandlerFn() 
    { }
}
JavaVM *javaVM;
JNIEnv *env;

// functions definitions
int findFunctionPtrSharedLib();
jint JNICALL GetEnv_fake(JavaVM *, void **, jint);
jint JNICALL AttachCurrentThread_fake(JavaVM *, JNIEnv **, void *);
jint RegisterNatives_fake(JNIEnv *, jclass , const JNINativeMethod *, jint);
jclass FindClass_fake(JNIEnv *, const char *);

// globals definitions
function_t *targetFunctionPtr;
std::string targetFunctionName;
std::string targetAppPath;


static auto load_art() -> std::pair<JavaVM *, JNIEnv *>
{
	std::string apk_path = "-Djava.class.path=" + targetAppPath + "/base.apk";
	std::string lib_path = "-Djava.library.path=" + targetAppPath + "/lib/arm64";
    JavaVMOption opt[] = {
    	{ apk_path.c_str(), nullptr},
        { lib_path.c_str(), nullptr}
    };


    JavaVMInitArgs args = {
        JNI_VERSION_1_6,
        std::size(opt),
        opt,
        JNI_FALSE
    };

    void * libart = dlopen("libart.so", RTLD_NOW);
    if (!libart) 
    {
        std::cerr << dlerror() << std::endl;
        abort();
    }

    void * libandroidruntime = dlopen("libandroid_runtime.so", RTLD_NOW);
    if (!libart) 
    {
        std::cerr << dlerror() << std::endl;
        abort();
    }
    
    auto JNI_CreateJavaVM = (JNI_CreateJavaVM_t *)dlsym(libart, "JNI_CreateJavaVM");
	
    if (!JNI_CreateJavaVM)
    {
        std::cerr << "No JNI_CreateJavaVM: " << dlerror() << std::endl;
        abort();
    }

    auto registerNatives = (registerNatives_t *)dlsym(libandroidruntime, "registerFrameworkNatives");
   	

    if (!registerNatives)
    {
        std::cerr << "No registerNatives: " << dlerror() << std::endl;
        abort();
    }


    std::pair<JavaVM *, JNIEnv *> ret;

    int res = JNI_CreateJavaVM(&ret.first, &ret.second, &args);
    if (res != 0)
    {
        std::cerr << "Failed to create VM: " << res << std::endl;
        abort();
    }

    auto [vm_tmp, env_tmp] = ret;

    jint res1 = registerNatives(env_tmp, 0);
    if (res1 != 0)
    {
        std::cerr << "Failed to call registerNatives: " << res1 << std::endl;
        abort();
    }

    return ret;
}


#endif

int main(int argc, char *argv[]) {
    #if defined(__ANDROID__)

    	// get target app path (e.g. target_APK/app_name)
    	targetAppPath = std::string(argv[1]);
    	std::cout << targetAppPath << std::endl;
    	
		/* Load ART
		 - call JNI_CreateJavaVM 
		 - link all insternal functions with registerNatives
		 */
        auto [javaVM_tmp, env_tmp] = load_art();
        javaVM = javaVM_tmp;
        env = env_tmp;


        /* Find target function pointer in shared library */
        // TODO add argc check
        targetFunctionName = std::string(argv[2]);
        //function_t *function = findFunctionPtrSharedLib("stringFromJNI");
        if (findFunctionPtrSharedLib() == 0) {
        	return 0;
        }
        // need to call JNI_OnLoad to set-up some required states
       	std::string path = targetAppPath + "/lib/arm64";
     	JNI_OnLoad_t *JNI_OnLoadPtr;

        for (const auto & entry: fs::directory_iterator(path)) {
	        void *lib = dlopen(entry.path().filename().c_str(), RTLD_NOW);
	        if (!lib) 
	        {
	            std::cerr << dlerror() << std::endl;
	            abort();
	        }

	        JNI_OnLoadPtr = (JNI_OnLoad_t *)dlsym(lib, "JNI_OnLoad");
	        if (JNI_OnLoadPtr)
	        {
	            JNI_OnLoadPtr(javaVM, NULL);
	        }
	        dlclose(lib);
    	}


        /* Init forkserver here */
       #ifdef __AFL_HAVE_MANUAL_CONTROL
          __AFL_INIT();
       #endif
		
        /* Get parameters */
        jclass MainActivityCls;
        jint info;
		std::string input[4];
		std::ifstream f(argv[3]);
		jstring jinput[4];
		int idx = 0;
		
		while(getline(f, input[idx])) {			
			// check if string is empty
			if (input[idx].empty()) {
				return 0;
			}

			// check if string contains spaces
			if (std::count(input[idx].begin(), input[idx].end(), ' ') != 0) {
				return 0;
			}
			
			jinput[idx] = env->NewStringUTF(input[idx].c_str());
			std::cout << input[idx] << std::endl;
			idx++;
			if (idx == 4) {
				break;
			}
		}

		// fuzzing input must have not more then 4 lines
		if (idx != 4) {
			return 0;
		}
      
        /* Fuzz */
		std::cout << "CALLING..." << std::endl;
        // call target function
        info = targetFunctionPtr(env, MainActivityCls, jinput[0], jinput[1], jinput[2], jinput[3]);

		return 0;
   #endif
}

#if defined(__ANDROID__)
int findFunctionPtrSharedLib()
{
	/* search in pattern-defined functions */
	std::string path = targetAppPath + "/lib/arm64";
    for (const auto & entry: fs::directory_iterator(path)) {
        void *lib = dlopen(entry.path().filename().c_str(), RTLD_NOW);
        if (!lib) 
        {
            std::cerr << dlerror() << std::endl;
            abort();
        }

        targetFunctionPtr = (function_t *)dlsym(lib, targetFunctionName.c_str());
        if (targetFunctionPtr)
        {
            return 1;
        }
        dlclose(lib);
    }

    /* search in static-defined functions */
	// duplicate JavaVM to inject fake RegisterNatives function
	JavaVM *javaVM_fake = (JavaVM *) malloc(sizeof(JavaVM));
	JNIInvokeInterface *javaVM_fake_functions = (JNIInvokeInterface *) malloc(sizeof(JNIInvokeInterface));
	JNIInvokeInterface *javaVM_functions = (JNIInvokeInterface *)javaVM->functions;
	JNI_OnLoad_t *JNI_OnLoadPtr;


	// define fake GetEnv
	javaVM_fake_functions->GetEnv = GetEnv_fake;

	// copy all original functions (or at least the minimum to pass JNI_OnLoad)
	javaVM_fake_functions->AttachCurrentThread = javaVM_functions->AttachCurrentThread;
	/*
    jint (JNICALL *DestroyJavaVM)(JavaVM *vm);
    jint (JNICALL *AttachCurrentThread)(JavaVM *vm, void **penv, void *args);
    jint (JNICALL *DetachCurrentThread)(JavaVM *vm);
    jint (JNICALL *GetEnv)(JavaVM *vm, void **penv, jint version);
    jint (JNICALL *AttachCurrentThreadAsDaemon)(JavaVM *vm, void **penv, void *args);
	*/

	javaVM_fake->functions = javaVM_fake_functions;

	// extract java side native method
	targetFunctionName = targetFunctionName.substr(targetFunctionName.rfind("_") + 1);

    for (const auto & entry: fs::directory_iterator(path)) {
        void *lib = dlopen(entry.path().filename().c_str(), RTLD_NOW);
        if (!lib) 
        {
            std::cerr << dlerror() << std::endl;
            abort();
        }

        JNI_OnLoadPtr = (JNI_OnLoad_t *)dlsym(lib, "JNI_OnLoad");
        if (JNI_OnLoadPtr)
        {
            JNI_OnLoadPtr(javaVM_fake, NULL);
            if (targetFunctionPtr) {
            	return 1;
            }
        }
        dlclose(lib);
    }
	
	std::cerr << "Function " << targetFunctionName << " to be fuzzed not found" << std::endl;

    return 0;
}

jint JNICALL GetEnv_fake(JavaVM *javaVM_fake, void **env_ret, jint status)
	{
		std::cout << "LOG: Fake GetEnv called" << std::endl;

		// define required structures
		JNIEnv *env_fake = (JNIEnv *) malloc(sizeof(JNIEnv));
		JNINativeInterface *env_fake_functions = (JNINativeInterface *) malloc(sizeof(JNINativeInterface));
		JNINativeInterface *env_functions = (JNINativeInterface *)env->functions;

		// define fake RegisterNatives
		env_fake_functions->RegisterNatives = RegisterNatives_fake;

		// copy all original functions (or at least the minimum to pass JNI_OnLoad)
		env_fake_functions->GetVersion = env_functions->GetVersion;
		env_fake_functions->DefineClass = env_functions->DefineClass;
		env_fake_functions->FindClass = FindClass_fake;
		env_fake_functions->FromReflectedMethod = env_functions->FromReflectedMethod;
		env_fake_functions->FromReflectedField = env_functions->FromReflectedField;
		env_fake_functions->ToReflectedMethod = env_functions->ToReflectedMethod;
		env_fake_functions->GetSuperclass = env_functions->GetSuperclass;
		env_fake_functions->IsAssignableFrom = env_functions->IsAssignableFrom;
		env_fake_functions->ToReflectedField = env_functions->ToReflectedField;
		env_fake_functions->Throw = env_functions->Throw;
		env_fake_functions->ThrowNew = env_functions->ThrowNew;
		env_fake_functions->ExceptionOccurred = env_functions->ExceptionOccurred;
		env_fake_functions->ExceptionDescribe = env_functions->ExceptionDescribe;
		env_fake_functions->ExceptionClear = env_functions->ExceptionClear;
		env_fake_functions->FatalError = env_functions->FatalError;
		env_fake_functions->PushLocalFrame = env_functions->PushLocalFrame;
		env_fake_functions->PopLocalFrame = env_functions->PopLocalFrame;
		env_fake_functions->NewGlobalRef = env_functions->NewGlobalRef;
		env_fake_functions->DeleteGlobalRef = env_functions->DeleteGlobalRef;
		env_fake_functions->DeleteLocalRef = env_functions->DeleteLocalRef;
		env_fake_functions->IsSameObject = env_functions->IsSameObject;
		env_fake_functions->NewLocalRef = env_functions->NewLocalRef;
		env_fake_functions->EnsureLocalCapacity = env_functions->EnsureLocalCapacity;
		env_fake_functions->AllocObject = env_functions->AllocObject;
		env_fake_functions->NewObject = env_functions->NewObject;
		env_fake_functions->NewObjectV = env_functions->NewObjectV;
		env_fake_functions->NewObjectA = env_functions->NewObjectA;
		env_fake_functions->GetObjectClass = env_functions->GetObjectClass;
		env_fake_functions->IsInstanceOf = env_functions->IsInstanceOf;
		env_fake_functions->GetMethodID = env_functions->GetMethodID;
		env_fake_functions->CallObjectMethod = env_functions->CallObjectMethod;
		env_fake_functions->CallObjectMethodV = env_functions->CallObjectMethodV;
		env_fake_functions->CallObjectMethodA = env_functions->CallObjectMethodA;
		env_fake_functions->CallBooleanMethod = env_functions->CallBooleanMethod;
		env_fake_functions->CallBooleanMethodV = env_functions->CallBooleanMethodV;
		env_fake_functions->CallBooleanMethodA = env_functions->CallBooleanMethodA;
		env_fake_functions->CallByteMethod = env_functions->CallByteMethod;
		env_fake_functions->CallByteMethodV = env_functions->CallByteMethodV;
		env_fake_functions->CallByteMethodA = env_functions->CallByteMethodA;
		env_fake_functions->CallCharMethod = env_functions->CallCharMethod;
		env_fake_functions->CallCharMethodV = env_functions->CallCharMethodV;
		env_fake_functions->CallCharMethodA = env_functions->CallCharMethodA;
		env_fake_functions->CallShortMethod = env_functions->CallShortMethod;
		env_fake_functions->CallShortMethodV = env_functions->CallShortMethodV;
		env_fake_functions->CallShortMethodA = env_functions->CallShortMethodA;
		env_fake_functions->CallIntMethod = env_functions->CallIntMethod;
		env_fake_functions->CallIntMethodV = env_functions->CallIntMethodV;
		env_fake_functions->CallIntMethodA = env_functions->CallIntMethodA;
		env_fake_functions->CallLongMethod = env_functions->CallLongMethod;
		env_fake_functions->CallLongMethodV = env_functions->CallLongMethodV;
		env_fake_functions->CallLongMethodA = env_functions->CallLongMethodA;
		env_fake_functions->CallFloatMethod = env_functions->CallFloatMethod;
		env_fake_functions->CallFloatMethodV = env_functions->CallFloatMethodV;
		env_fake_functions->CallFloatMethodA = env_functions->CallFloatMethodA;
		env_fake_functions->CallDoubleMethod = env_functions->CallDoubleMethod;
		env_fake_functions->CallDoubleMethodV = env_functions->CallDoubleMethodV;
		env_fake_functions->CallDoubleMethodA = env_functions->CallDoubleMethodA;
		env_fake_functions->CallVoidMethod = env_functions->CallVoidMethod;
		env_fake_functions->CallVoidMethodV = env_functions->CallVoidMethodV;
		env_fake_functions->CallVoidMethodA = env_functions->CallVoidMethodA;
		env_fake_functions->CallNonvirtualObjectMethod = env_functions->CallNonvirtualObjectMethod;
		env_fake_functions->CallNonvirtualObjectMethodV = env_functions->CallNonvirtualObjectMethodV;
		env_fake_functions->CallNonvirtualObjectMethodA = env_functions->CallNonvirtualObjectMethodA;
		env_fake_functions->CallNonvirtualBooleanMethod = env_functions->CallNonvirtualBooleanMethod;
		env_fake_functions->CallNonvirtualBooleanMethodV = env_functions->CallNonvirtualBooleanMethodV;
		env_fake_functions->CallNonvirtualBooleanMethodA = env_functions->CallNonvirtualBooleanMethodA;
		env_fake_functions->CallNonvirtualByteMethod = env_functions->CallNonvirtualByteMethod;
		env_fake_functions->CallNonvirtualByteMethodV = env_functions->CallNonvirtualByteMethodV;
		env_fake_functions->CallNonvirtualByteMethodA = env_functions->CallNonvirtualByteMethodA;
		env_fake_functions->CallNonvirtualCharMethod = env_functions->CallNonvirtualCharMethod;
		env_fake_functions->CallNonvirtualCharMethodV = env_functions->CallNonvirtualCharMethodV;
		env_fake_functions->CallNonvirtualCharMethodA = env_functions->CallNonvirtualCharMethodA;
		env_fake_functions->CallNonvirtualShortMethod = env_functions->CallNonvirtualShortMethod;
		env_fake_functions->CallNonvirtualShortMethodV = env_functions->CallNonvirtualShortMethodV;
		env_fake_functions->CallNonvirtualShortMethodA = env_functions->CallNonvirtualShortMethodA;
		env_fake_functions->CallNonvirtualIntMethod = env_functions->CallNonvirtualIntMethod;
		env_fake_functions->CallNonvirtualIntMethodV = env_functions->CallNonvirtualIntMethodV;
		env_fake_functions->CallNonvirtualIntMethodA = env_functions->CallNonvirtualIntMethodA;
		env_fake_functions->CallNonvirtualLongMethod = env_functions->CallNonvirtualLongMethod;
		env_fake_functions->CallNonvirtualLongMethodV = env_functions->CallNonvirtualLongMethodV;
		env_fake_functions->CallNonvirtualLongMethodA = env_functions->CallNonvirtualLongMethodA;
		env_fake_functions->CallNonvirtualFloatMethod = env_functions->CallNonvirtualFloatMethod;
		env_fake_functions->CallNonvirtualFloatMethodV = env_functions->CallNonvirtualFloatMethodV;
		env_fake_functions->CallNonvirtualFloatMethodA = env_functions->CallNonvirtualFloatMethodA;
		env_fake_functions->CallNonvirtualDoubleMethod = env_functions->CallNonvirtualDoubleMethod;
		env_fake_functions->CallNonvirtualDoubleMethodV = env_functions->CallNonvirtualDoubleMethodV;
		env_fake_functions->CallNonvirtualDoubleMethodA = env_functions->CallNonvirtualDoubleMethodA;
		env_fake_functions->CallNonvirtualVoidMethod = env_functions->CallNonvirtualVoidMethod;
		env_fake_functions->CallNonvirtualVoidMethodV = env_functions->CallNonvirtualVoidMethodV;
		env_fake_functions->CallNonvirtualVoidMethodA = env_functions->CallNonvirtualVoidMethodA;
		env_fake_functions->GetFieldID = env_functions->GetFieldID;
		env_fake_functions->GetObjectField = env_functions->GetObjectField;
		env_fake_functions->GetBooleanField = env_functions->GetBooleanField;
		env_fake_functions->GetByteField = env_functions->GetByteField;
		env_fake_functions->GetCharField = env_functions->GetCharField;
		env_fake_functions->GetShortField = env_functions->GetShortField;
		env_fake_functions->GetIntField = env_functions->GetIntField;
		env_fake_functions->GetLongField = env_functions->GetLongField;
		env_fake_functions->GetFloatField = env_functions->GetFloatField;
		env_fake_functions->GetDoubleField = env_functions->GetDoubleField;
		env_fake_functions->SetObjectField = env_functions->SetObjectField;
		env_fake_functions->SetBooleanField = env_functions->SetBooleanField;
		env_fake_functions->SetByteField = env_functions->SetByteField;
		env_fake_functions->SetCharField = env_functions->SetCharField;
		env_fake_functions->SetShortField = env_functions->SetShortField;
		env_fake_functions->SetIntField = env_functions->SetIntField;
		env_fake_functions->SetLongField = env_functions->SetLongField;
		env_fake_functions->SetFloatField = env_functions->SetFloatField;
		env_fake_functions->SetDoubleField = env_functions->SetDoubleField;
		env_fake_functions->GetStaticMethodID = env_functions->GetStaticMethodID;
		env_fake_functions->CallStaticObjectMethod = env_functions->CallStaticObjectMethod;
		env_fake_functions->CallStaticObjectMethodV = env_functions->CallStaticObjectMethodV;
		env_fake_functions->CallStaticObjectMethodA = env_functions->CallStaticObjectMethodA;
		env_fake_functions->CallStaticBooleanMethod = env_functions->CallStaticBooleanMethod;
		env_fake_functions->CallStaticBooleanMethodV = env_functions->CallStaticBooleanMethodV;
		env_fake_functions->CallStaticBooleanMethodA = env_functions->CallStaticBooleanMethodA;
		env_fake_functions->CallStaticByteMethod = env_functions->CallStaticByteMethod;
		env_fake_functions->CallStaticByteMethodV = env_functions->CallStaticByteMethodV;
		env_fake_functions->CallStaticByteMethodA = env_functions->CallStaticByteMethodA;
		env_fake_functions->CallStaticCharMethod = env_functions->CallStaticCharMethod;
		env_fake_functions->CallStaticCharMethodV = env_functions->CallStaticCharMethodV;
		env_fake_functions->CallStaticCharMethodA = env_functions->CallStaticCharMethodA;
		env_fake_functions->CallStaticShortMethod = env_functions->CallStaticShortMethod;
		env_fake_functions->CallStaticShortMethodV = env_functions->CallStaticShortMethodV;
		env_fake_functions->CallStaticShortMethodA = env_functions->CallStaticShortMethodA;
		env_fake_functions->CallStaticIntMethod = env_functions->CallStaticIntMethod;
		env_fake_functions->CallStaticIntMethodV = env_functions->CallStaticIntMethodV;
		env_fake_functions->CallStaticIntMethodA = env_functions->CallStaticIntMethodA;
		env_fake_functions->CallStaticLongMethod = env_functions->CallStaticLongMethod;
		env_fake_functions->CallStaticLongMethodV = env_functions->CallStaticLongMethodV;
		env_fake_functions->CallStaticLongMethodA = env_functions->CallStaticLongMethodA;
		env_fake_functions->CallStaticFloatMethod = env_functions->CallStaticFloatMethod;
		env_fake_functions->CallStaticFloatMethodV = env_functions->CallStaticFloatMethodV;
		env_fake_functions->CallStaticFloatMethodA = env_functions->CallStaticFloatMethodA;
		env_fake_functions->CallStaticDoubleMethod = env_functions->CallStaticDoubleMethod;
		env_fake_functions->CallStaticDoubleMethodV = env_functions->CallStaticDoubleMethodV;
		env_fake_functions->CallStaticDoubleMethodA = env_functions->CallStaticDoubleMethodA;
		env_fake_functions->CallStaticVoidMethod = env_functions->CallStaticVoidMethod;
		env_fake_functions->CallStaticVoidMethodV = env_functions->CallStaticVoidMethodV;
		env_fake_functions->CallStaticVoidMethodA = env_functions->CallStaticVoidMethodA;
		env_fake_functions->GetStaticFieldID = env_functions->GetStaticFieldID;
		env_fake_functions->GetStaticObjectField = env_functions->GetStaticObjectField;
		env_fake_functions->GetStaticBooleanField = env_functions->GetStaticBooleanField;
		env_fake_functions->GetStaticByteField = env_functions->GetStaticByteField;
		env_fake_functions->GetStaticCharField = env_functions->GetStaticCharField;
		env_fake_functions->GetStaticShortField = env_functions->GetStaticShortField;
		env_fake_functions->GetStaticIntField = env_functions->GetStaticIntField;
		env_fake_functions->GetStaticLongField = env_functions->GetStaticLongField;
		env_fake_functions->GetStaticFloatField = env_functions->GetStaticFloatField;
		env_fake_functions->GetStaticDoubleField = env_functions->GetStaticDoubleField;
		env_fake_functions->SetStaticObjectField = env_functions->SetStaticObjectField;
		env_fake_functions->SetStaticBooleanField = env_functions->SetStaticBooleanField;
		env_fake_functions->SetStaticByteField = env_functions->SetStaticByteField;
		env_fake_functions->SetStaticCharField = env_functions->SetStaticCharField;
		env_fake_functions->SetStaticShortField = env_functions->SetStaticShortField;
		env_fake_functions->SetStaticIntField = env_functions->SetStaticIntField;
		env_fake_functions->SetStaticLongField = env_functions->SetStaticLongField;
		env_fake_functions->SetStaticFloatField = env_functions->SetStaticFloatField;
		env_fake_functions->SetStaticDoubleField = env_functions->SetStaticDoubleField;
		env_fake_functions->NewString = env_functions->NewString;
		env_fake_functions->GetStringLength = env_functions->GetStringLength;
		env_fake_functions->GetStringChars = env_functions->GetStringChars;
		env_fake_functions->ReleaseStringChars = env_functions->ReleaseStringChars;
		env_fake_functions->NewStringUTF = env_functions->NewStringUTF;
		env_fake_functions->GetStringUTFLength = env_functions->GetStringUTFLength;
		env_fake_functions->GetStringUTFChars = env_functions->GetStringUTFChars;
		env_fake_functions->ReleaseStringUTFChars = env_functions->ReleaseStringUTFChars;
		env_fake_functions->GetArrayLength = env_functions->GetArrayLength;
		env_fake_functions->NewObjectArray = env_functions->NewObjectArray;
		env_fake_functions->GetObjectArrayElement = env_functions->GetObjectArrayElement;
		env_fake_functions->SetObjectArrayElement = env_functions->SetObjectArrayElement;
		env_fake_functions->NewBooleanArray = env_functions->NewBooleanArray;
		env_fake_functions->NewByteArray = env_functions->NewByteArray;
		env_fake_functions->NewCharArray = env_functions->NewCharArray;
		env_fake_functions->NewShortArray = env_functions->NewShortArray;
		env_fake_functions->NewIntArray = env_functions->NewIntArray;
		env_fake_functions->NewLongArray = env_functions->NewLongArray;
		env_fake_functions->NewFloatArray = env_functions->NewFloatArray;
		env_fake_functions->NewDoubleArray = env_functions->NewDoubleArray;
		env_fake_functions->GetBooleanArrayElements = env_functions->GetBooleanArrayElements;
		env_fake_functions->GetByteArrayElements = env_functions->GetByteArrayElements;
		env_fake_functions->GetCharArrayElements = env_functions->GetCharArrayElements;
		env_fake_functions->GetShortArrayElements = env_functions->GetShortArrayElements;
		env_fake_functions->GetIntArrayElements = env_functions->GetIntArrayElements;
		env_fake_functions->GetLongArrayElements = env_functions->GetLongArrayElements;
		env_fake_functions->GetFloatArrayElements = env_functions->GetFloatArrayElements;
		env_fake_functions->GetDoubleArrayElements = env_functions->GetDoubleArrayElements;
		env_fake_functions->ReleaseBooleanArrayElements = env_functions->ReleaseBooleanArrayElements;
		env_fake_functions->ReleaseByteArrayElements = env_functions->ReleaseByteArrayElements;
		env_fake_functions->ReleaseCharArrayElements = env_functions->ReleaseCharArrayElements;
		env_fake_functions->ReleaseShortArrayElements = env_functions->ReleaseShortArrayElements;
		env_fake_functions->ReleaseIntArrayElements = env_functions->ReleaseIntArrayElements;
		env_fake_functions->ReleaseLongArrayElements = env_functions->ReleaseLongArrayElements;
		env_fake_functions->ReleaseFloatArrayElements = env_functions->ReleaseFloatArrayElements;
		env_fake_functions->ReleaseDoubleArrayElements = env_functions->ReleaseDoubleArrayElements;
		env_fake_functions->GetBooleanArrayRegion = env_functions->GetBooleanArrayRegion;
		env_fake_functions->GetByteArrayRegion = env_functions->GetByteArrayRegion;
		env_fake_functions->GetCharArrayRegion = env_functions->GetCharArrayRegion;
		env_fake_functions->GetShortArrayRegion = env_functions->GetShortArrayRegion;
		env_fake_functions->GetIntArrayRegion = env_functions->GetIntArrayRegion;
		env_fake_functions->GetLongArrayRegion = env_functions->GetLongArrayRegion;
		env_fake_functions->GetFloatArrayRegion = env_functions->GetFloatArrayRegion;
		env_fake_functions->GetDoubleArrayRegion = env_functions->GetDoubleArrayRegion;
		env_fake_functions->SetBooleanArrayRegion = env_functions->SetBooleanArrayRegion;
		env_fake_functions->SetByteArrayRegion = env_functions->SetByteArrayRegion;
		env_fake_functions->SetCharArrayRegion = env_functions->SetCharArrayRegion;
		env_fake_functions->SetShortArrayRegion = env_functions->SetShortArrayRegion;
		env_fake_functions->SetIntArrayRegion = env_functions->SetIntArrayRegion;
		env_fake_functions->SetLongArrayRegion = env_functions->SetLongArrayRegion;
		env_fake_functions->SetFloatArrayRegion = env_functions->SetFloatArrayRegion;
		env_fake_functions->SetDoubleArrayRegion = env_functions->SetDoubleArrayRegion;
		env_fake_functions->UnregisterNatives = env_functions->UnregisterNatives;
		env_fake_functions->MonitorEnter = env_functions->MonitorEnter;
		env_fake_functions->MonitorExit = env_functions->MonitorExit;
		env_fake_functions->GetJavaVM = env_functions->GetJavaVM;
		env_fake_functions->GetStringRegion = env_functions->GetStringRegion;
		env_fake_functions->GetStringUTFRegion = env_functions->GetStringUTFRegion;
		env_fake_functions->GetPrimitiveArrayCritical = env_functions->GetPrimitiveArrayCritical;
		env_fake_functions->ReleasePrimitiveArrayCritical = env_functions->ReleasePrimitiveArrayCritical;
		env_fake_functions->GetStringCritical = env_functions->GetStringCritical;
		env_fake_functions->ReleaseStringCritical = env_functions->ReleaseStringCritical;
		env_fake_functions->NewWeakGlobalRef = env_functions->NewWeakGlobalRef;
		env_fake_functions->DeleteWeakGlobalRef = env_functions->DeleteWeakGlobalRef;
		env_fake_functions->ExceptionCheck = env_functions->ExceptionCheck;
		env_fake_functions->NewDirectByteBuffer = env_functions->NewDirectByteBuffer;
		env_fake_functions->GetDirectBufferAddress = env_functions->GetDirectBufferAddress;
		env_fake_functions->GetDirectBufferCapacity = env_functions->GetDirectBufferCapacity;
		env_fake_functions->GetObjectRefType = env_functions->GetObjectRefType;

		env_fake->functions = env_fake_functions;

		*env_ret = (void *)env_fake;
		
		return JNI_OK;
	}

jint JNICALL AttachCurrentThread_fake(JavaVM *vm, JNIEnv **penv, void *args)
	{
		std::cout << "LOG: Fake AttachCurrentThread called" << std::endl;

		if ((vm)->GetEnv((void **)penv, JNI_VERSION_1_6) != JNI_OK) {
			return JNI_ERR;
		}
		return JNI_OK;
	}

jint RegisterNatives_fake(JNIEnv *penv, jclass clazz, const JNINativeMethod *methods, jint nMethods)
	{
		std::cout << "LOG: Fake RegisterNatives called" << std::endl;

		for (int i = 0; i < (int)nMethods; i++) {
			std::cout << "	Method " << i << std::endl;
			std::cout << "	├── name(Java): " << methods[i].name << std::endl;
			std::cout << "	├── signature: " << methods[i].signature << std::endl;
			std::cout << "	└── pointer(Native): "<< methods[i].fnPtr << std::endl;
			
			if (std::string(methods[i].name).compare(targetFunctionName) == 0) {
				targetFunctionPtr = (function_t *) methods[i].fnPtr;
				return -1;
			}
		}

		// terminate program after having fetched all native methods
		return -1;
	}

jclass FindClass_fake(JNIEnv *penv, const char *class_name)
	{
		jclass clazz = env->FindClass(class_name);

		std::cout << "LOG: Fake FindClass called (class: " << class_name << ")" << std::endl;

		return clazz;	
	}

#endif
