# Android Native Componet Fuzzing Framework

Fuzzing framework to target native components of Android APK, either targeting all functions with a given signature or a specific function

## Requirements
* Android devices must be rooted

* All Android devices must have a built version of *AFLplusplus-AndroidPatches* (get it from [here](https://github.com/paocela/AFLplusplus-AndroidPatches))

* **jadx** built from source:

  ```
  cd jadx
  ./gradlew dist
  ```

* to use only after running the script `analyze_native_signatures.sh`: this guarantees that `/target_APK` folder structure is (for each app):

  ```
  ## Before Analysis ##
  ├── target_APK/
  │   ├── App-Name/
  │   │	└── base.apk
  │   └── ...
  
  ## After Analysis ##
  ├── target_APK/
  │   ├── App-Name/
  │   │	├── base/
  │   │	├── lib/
  │   │   │   └── arm64-v8a/
  │   │	├── base.apk
  │   │	└── signatures_pattern.txt
  │   └── ...
  ```

* `harness.cpp` written based on the target choice

## Usage

### analyze_native_signature.sh

```
Syntax: ./analyze_native_signatures.sh [-h] [qdox|grep]

Analyze APK native methods signatures, group by and count them

Requirements:
   - target_APK folder structure:
      ├── target_APK/
      │   ├── App-Name/
      │   │	  └── base.apk
      │   └── ...

Options:
   -h, --help     Print this Help.
   [qdox|grep]    Choose static analysis method (only qdox implemented, it's more precise)
```

### fuzzing_driver.sh

```
Syntax: ./fuzzing_driver.sh <signature-chosen> <time-to-fuzz> <input-dir> <output-dir> <read-from-file[0|1]> <AFL_DEBUG[0|1]>

Fuzz native methods of different APKs with given signature

Options:
   -h, --help     Print this Help.
   <signature-chosen>: signature chosen from *analyze_native_signatures.sh* script, as fuzzing target
   <time-to-fuzz>: time to fuzz each method for, as float[s|m|h|d] (s=seconds, m=minutes, h=hours, d=days)
   <input-dir>: fuzzing input directory name, populated with meaningful seeds
   <output-dir>: fuzzing output directory name
   <read-from-file>: flag to specify if fuzzer will read from file or from stdin (depending on how harness is implemented)
   <AFL_DEBUG[0|1]>: set if you want to debug AFL++
   <parallel-fuzzing[0...#max_cores]>: Specify number N of cores to use for a parallel fuzzing campaign (if N > #cores, then max #cores is used)
```

### fuzzing_one.sh

```
Syntax: ./fuzzing_one.sh <method-chosen> <time-to-fuzz> <input-dir> <output-dir> <read-from-file[0|1]> <AFL_DEBUG[0|1]>

Fuzz given native method

Options:
   -h, --help     Print this Help.
   <method-chosen>: method chosen from *analyze_native_signatures.sh* script, as fuzzing target
   <time-to-fuzz>: time to fuzz each method for, as float[s|m|h|d] (s=seconds, m=minutes, h=hours, d=days)
   <input-dir>: fuzzing input directory name, populated with meaningful seeds
   <output-dir>: fuzzing output directory name
   <read-from-file>: flag to specify if fuzzer will read from file or from stdin (depending on how harness is implemented)
   <AFL_DEBUG[0|1]>: set if you want to debug AFL++
```

### Steps to write harness

1. modify target function signature (`harness.cpp:21`)
2. write harness (`harness.cpp:84`)
   1. read data from `stdin` or file
   2. call target function

### Debug POC

```bash
# in APK_signature_analysis_and_fuzzer, do:
$ export LD_PRELOAD=$(pwd)/../AFLplusplus-AndroidPatches/libLLVM-13.so:$LD_PRELOAD
$ LD_LIBRARY_PATH=/apex/com.android.art/lib64:$(pwd)/target_APK/<app_name>/lib/arm64-v8a:/system/lib64 gdb --args ./harness target_APK/<app_name> <target_function_name> fuzz_output/<path/to/POC>
```

## Components

```
.
├── extractor_pattern/
│   ├── com.qdox.jar
│   ├── extractor.java
│   ├── extract_patter_native.sh
│   └── README.md
├── fuzz_dir/
├── fuzz_input/
├── fuzz_output/
├── harnesses/
│   ├── harness_int:string_string_string.cpp
│   └── ...
├── jadx/
├── target_APK/
│   ├── App-Name/
│   │	├── base/
│   │	├── lib/
│   │	├── base.apk
│   │	└── signatures_pattern.txt
│   └── ...
├── analyze_native_signatures.sh
├── fuzzing_driver.sh
├── fuzzing_one.sh
├── harness.cpp
├── harness.h
└── README.md
```

* **/extractor_pattern**: contains framework to extract native methods name and signatures from decompiled APK
* **/fuzz_dir**: fuzzing directory (should be empty)
* **/fuzz_input**: AFL++ input directory, contains interesting non-crashing seeds
* **/fuzz_output**: AFL++ output directory, will contain whatever AFL++ will produce (hopefully some crashes)
* **/harnesses**: contains previously written harnesses for some function signatures
* **/jadx**: Java decompiler
* **/target_APK**: folder containing APK under analysis
* **analyze_native_signature.sh**: driver to call extractor framework (`/extractor_pattern`)
* **fuzzing_driver.sh**: fuzzing driver to fuzz all native methods contained in `/target_APK` apps with given signature
* **fuzzing_one.sh**: fuzzing driver to fuzz a single method contained in `/target_APK` app
* **harness.cpp**: harness source file, should be populated depending on the target function signature
* **harness.h**: harness source file header
* **README.md**: this README
