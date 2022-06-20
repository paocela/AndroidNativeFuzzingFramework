# Fuzz Android Native Components on Phone Cluster

Framework built to fuzz on multiple devices one or a set of functions, which are part of the native component of an Android APK
Devices are connect to the central machine over the same network, using ADB

## Requirements
* All Android devices must be rooted

* All Android devices must be have a connection with the central machine through ADB. Steps (source [here](https://stackoverflow.com/questions/43973838/how-to-connect-multiple-android-devices-with-adb-over-wifi)):
  1. connect device with USB cable to PC
  2. `adb -d tcpip 5555`
  3. `adb connect <device_ip_addr>`
  4. repeat for all other devices
  
* All Android devices must have a built version of *AFLplusplus-AndroidPatches* (get it from [here](https://github.com/paocela/AFLplusplus-AndroidPatches))

* to use only after running the script `analyze_native_signatures.sh` in folder `/APK_signature_analysis_and_fuzzer`: this guarantees that `/target_APK` folder structure is (for each app):

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

* `APK_signature_analysis_and_fuzzer/harness.cpp` written based on the target choice

## Usage

```
python fuzzing_manager.py [-h] --action {fuzz_signature,fuzz_one,check} [--target TARGET]
                          [--fuzz_time FUZZ_TIME] [--from_stdin FROM_STDIN] [--parallel_fuzzing PARALLEL_FUZZING]


Fuzz Android native libraries functions with given signature on multiple devices through ADB

optional arguments:
  -h, --help            show this help message and exit
  --action {fuzz_signature,fuzz_one,check}
                        *fuzz* to fuzz on multiple devices, *check* to check on each fuzzing campaing
  --target TARGET       Fuzzing target signature or method, e.g. String:String,Int, or Java_...
                        (depending on --action)
  --fuzz_time FUZZ_TIME
                        Time to fuzz for, of type float[s|m|h|d] (s=seconds, m=minutes, h=hours,
                        d=days)
  --from_stdin FROM_STDIN
                        If True, harness get AFL++ input from stdin
  --parallel_fuzzing PARALLEL_FUZZING
                        Specify number N of cores to use for a parallel fuzzing campaign (if N > #cores, then max #cores is used)
```

## Components

```
.
├── APK_signature_analysis_and_fuzzer/
├── Root-Samsung-A40/
├── adb.py
├── fuzzing_manager.py
└── README.md
```

* **/APK_signature_analysis_and_fuzzer**: contains the actual fuzzing framework, composed of APK static analysis tools (signature extractor), harnesses, fuzzing driver and relative folders
* **/Root-Samsung-A40**: steps to root a Samsung-A40 phone
* **adb.py**: python library to integrate ADB commands
* **fuzzing_manager.py**: manage interaction with all devices connected, start fuzzing campains and fetch intermediary results
* **README.md**: this README
