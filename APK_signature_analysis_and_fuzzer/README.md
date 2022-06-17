# Android Native Componet Fuzzing Framework

Fuzzing framework to target native components of Android APK, either targeting all functions with a given signature or a specific function

## Requirements
* Android devices must be rooted
* All Android devices must have a built version of *AFLplusplus-AndroidPatches* (get it from [here](https://github.com/paocela/AFLplusplus-AndroidPatches))

## Usage

### analyze_native_signature.sh

```
Syntax: ./analyze_native_signatures [-h] [qdox|grep]

Analyze APK native methods signatures, group by and count them

Options:
   -h, --help     Print this Help.
   [qdox|grep]    Choose static analysis method (only qdox implemented, it's more precise)
```

### fuzzing_driver.sh

```
Syntax: ./fuzzing_driver <signature-chosen> <time-to-fuzz> <input-dir> <output-dir> <read-from-file[0|1]> <AFL_DEBUG[0|1]>

Fuzz native methods of different APKs with given signature

Options:
   -h, --help     Print this Help.
	<signature-chosen>: signature chosen from *analyze_native_signatures.sh* script, as fuzzing target
	<time-to-fuzz>: time to fuzz each method for, as float[s|m|h|d] (s=seconds, m=minutes, h=hours, d=days)
	<input-dir>: fuzzing input directory name, populated with meaningful seeds
	<output-dir>: fuzzing output directory name
	<read-from-file>: flag to specify if fuzzer will read from file or from stdin (depending on how harness is implemented)
	<AFL_DEBUG[0|1]>: set if you want to debug AFL++
```

### fuzzing_one.sh

```
Syntax: ./fuzzing_one <method-chosen> <time-to-fuzz> <input-dir> <output-dir> <read-from-file[0|1]> <AFL_DEBUG[0|1]>

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

## Components

```
.
├── extractor_pattern
├── fuzz_dir
├── fuzz_input
├── fuzz_output
├── harnesses
├── jadx
├── target_APK
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
* **analyze_native_signature.sh**: driver to call extractor framework (*/extractor_pattern*)
* **fuzzing_driver.sh**: fuzzing driver to fuzz all native methods contained in *target_APK* apps with given signature
* **fuzzing_one.sh**: fuzzing driver to fuzz a single method contained in target_APK app
* **harness.cpp**: harness source file, should be populated depending on the target function signature
* **harness.h**: harness source file header
* **README.md**: this README
