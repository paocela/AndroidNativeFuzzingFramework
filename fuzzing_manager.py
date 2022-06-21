from adb import *
import argparse
import re
import os
import shutil
from statistics import mean
import re

""" connect multiple devices over WIFI with ADB --> (source: https://stackoverflow.com/questions/43973838/how-to-connect-multiple-android-devices-with-adb-over-wifi)
1. connect device with USB cable to PC
2. adb -d tcpip 5555
3. adb connect 192.168.1.32
4. repeat for all other devices
"""

HOME_DIRECTORY = "/data/data/com.termux/files/home/"
HARNESS_FOLDER_ZIP = "APK_signature_analysis_and_fuzzer.zip"
HARNESS_FOLDER = "APK_signature_analysis_and_fuzzer/"

RED='\033[0;31m'
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
NC='\033[0m'

def fuzz_signature(sig, fuzz_time, from_stdin, parallel_fuzzing):
    for device_id in get_device_ids():
        print(f"{GREEN}[LOG] {NC}Handling device {device_id.decode('utf-8')}")

        # push folder
        print(f"{GREEN}[LOG] {NC}Pushing file {HARNESS_FOLDER_ZIP}")
        execute_privileged_command(" ".join(["rm", "-rf", HOME_DIRECTORY + HARNESS_FOLDER]), device_id=device_id)
        push_privileged(HARNESS_FOLDER_ZIP, HOME_DIRECTORY, is_directory=False, device_id=device_id)

        # unzip it
        execute_privileged_command(" ".join(["unzip", HOME_DIRECTORY + HARNESS_FOLDER_ZIP, "-d", HOME_DIRECTORY]), device_id=device_id)
        execute_privileged_command(" ".join(["rm", HOME_DIRECTORY + HARNESS_FOLDER_ZIP]), device_id=device_id)

        # set PERFORMANCE to CPU frequency scaling
        print(f"{GREEN}[LOG] {NC}Setting CPU freq scaling to PERFORMANCE")
        execute_privileged_command("cd /sys/devices/system/cpu && echo performance | tee cpu*/cpufreq/scaling_governor", device_id=device_id)

        # start fuzzing driver in background
        print(f"{GREEN}[LOG] {NC}Starting fuzzing...")
        execute_privileged_command('export PATH=/data/data/com.termux/files/usr/bin:$PATH && cd ' + HOME_DIRECTORY + HARNESS_FOLDER +' && nohup ./fuzzing_driver.sh ' + " ".join([sig, fuzz_time, "fuzz_input", "fuzz_output", str(int(from_stdin)), "0", str(int(parallel_fuzzing))]) + ' > /dev/null &', device_id=device_id, wait_for_termination=False)

        print(f"{GREEN}[LOG] {NC}Done!")

def fuzz_one(method, fuzz_time, from_stdin, parallel_fuzzing):
    for device_id in get_device_ids():
        print(f"{GREEN}[LOG] {NC}Handling device {device_id.decode('utf-8')}")

        # push folder
        print(f"{GREEN}[LOG] {NC}Pushing file {HARNESS_FOLDER_ZIP}")
        execute_privileged_command(" ".join(["rm", "-rf", HOME_DIRECTORY + HARNESS_FOLDER]), device_id=device_id)
        push_privileged(HARNESS_FOLDER_ZIP, HOME_DIRECTORY, is_directory=False, device_id=device_id)

        # unzip it
        execute_privileged_command(" ".join(["unzip", HOME_DIRECTORY + HARNESS_FOLDER_ZIP, "-d", HOME_DIRECTORY]), device_id=device_id)
        execute_privileged_command(" ".join(["rm", HOME_DIRECTORY + HARNESS_FOLDER_ZIP]), device_id=device_id)

        # set PERFORMANCE to CPU frequency scaling
        print(f"{GREEN}[LOG] {NC}Setting CPU freq scaling to PERFORMANCE")
        execute_privileged_command("cd /sys/devices/system/cpu && echo performance | tee cpu*/cpufreq/scaling_governor", device_id=device_id)

        # start fuzzing driver in background
        print(f"{GREEN}[LOG] {NC}Starting fuzzing...")
        execute_privileged_command('export PATH=/data/data/com.termux/files/usr/bin:$PATH && cd ' + HOME_DIRECTORY + HARNESS_FOLDER +' && nohup ./fuzzing_one.sh ' + " ".join([method, fuzz_time, "fuzz_input", "fuzz_output", str(int(from_stdin)), "0", str(int(parallel_fuzzing))]) + ' > /dev/null &', device_id=device_id, wait_for_termination=False)

        print(f"{GREEN}[LOG] {NC}Done!")

def check():
    print(f"{GREEN}[LOG] {NC}Pulling fuzzing output directory...")
    shutil.rmtree('./fuzz_check')
    os.makedirs("./fuzz_check")
    for device_id in get_device_ids():
        os.makedirs("./fuzz_check/" + device_id.decode('utf-8'), exist_ok=True)
        pull_privileged(HOME_DIRECTORY + HARNESS_FOLDER + "fuzz_output", "./fuzz_check/" + device_id.decode('utf-8'), is_directory=True, device_id=device_id)

        # show stats
        # total number crashes
        for target_function in os.listdir("./fuzz_check/" + device_id.decode('utf-8') + "/fuzz_output"):
            NUM_CRASHES = 0
            EXEC_PER_SEC = []
            RUNTIME = 0
            for core_output in os.listdir("./fuzz_check/" + device_id.decode('utf-8') + "/fuzz_output/" + target_function):
                # number crashes per core
                NUM_CRASHES += len([name for name in os.listdir("./fuzz_check/" + device_id.decode('utf-8') + "/fuzz_output/" + target_function + "/" + core_output + '/crashes/') if os.path.isfile(name)])

                # number execs per core
                f = open("./fuzz_check/" + device_id.decode('utf-8') + "/fuzz_output/" + target_function + "/" + core_output + "/fuzzer_stats")
                lines = f.readlines()
                EXEC_PER_SEC.append(float(lines[7].split(":")[-1]))

                # total runtime in seconds
                RUNTIME = int(lines[2].split(":")[-1])
                
            print(f"{YELLOW}[STATS] {NC}Function:{target_function} - Device:{device_id.decode('utf-8')}")
            print(f"   ├── #crashes = {NUM_CRASHES}")
            print(f"   ├── runtime (sec) = {RUNTIME}")
            print(f"   └── exec/s = {EXEC_PER_SEC} (avg = {mean(EXEC_PER_SEC)})")

    print(f"{GREEN}[LOG] {NC}Done! (find all output in ./fuzz_check)")

def kill_fuzzer(target):
    TARGETS = []
    if (target is None):
        TARGETS = list(map(lambda x: x.decode('utf-8'), get_device_ids()))
    else:
        TARGETS.append(target)

    for device_id in TARGETS:
        print(f"{GREEN}[LOG] {NC}Fetching PIDs...")
        ALL_PROC = execute_privileged_command("ps -ef | grep afl-fuzz | grep -v grep | grep -v timeout", device_id=device_id)[0].decode('utf-8').split("\n")[:-1]
        ALL_PROC = list(map(lambda x: re.sub("\s+", " ", x).split(" ")[1], ALL_PROC))
        if not ALL_PROC:
            print(f"{YELLOW}[WRN] {NC}No running fuzzers found (you sure?)")
            return
        for P in ALL_PROC:
            execute_privileged_command("kill -9 " + P, device_id=device_id)
            print(f"{GREEN}[LOG] {NC}Killed {P}")
        
    print(f"{GREEN}[LOG] {NC}Done!")


if __name__ == "__main__":
    
    parser = argparse.ArgumentParser(description='Fuzz Android native libraries functions with given signature on multiple devices through ADB')

    parser.add_argument("--action", type=str, choices=["fuzz_signature", "fuzz_one", "check", "kill_fuzzer"], required=True, help="*fuzz_signature* to fuzz all functions given a signature, *fuzz_one* to fuzz given function name, *check* to check on each fuzzing campaing, *kill_fuzzer* to kill all or devices running campaigns")
    parser.add_argument("--target", type=str, required=False, help="Fuzzing target signature or method, or device to kill, e.g. String:String,Int, or Java_... or 192.168... (depending on --action)")
    parser.add_argument("--fuzz_time", type=str, required=False, help="Time to fuzz for, of type float[s|m|h|d] (s=seconds, m=minutes, h=hours, d=days)")
    parser.add_argument("--from_stdin", type=bool, required=False, default=False, help="If True, harness get AFL++ input from stdin")
    parser.add_argument("--parallel_fuzzing", type=int, required=False, default=0, help="Specify number N of cores to use for a parallel fuzzing campaign (if N > #cores, then max #cores is used)")

    args = parser.parse_args()

    if args.action == "fuzz_signature" or args.action == "fuzz_one":

        # zip harness folder directory
        shutil.make_archive(HARNESS_FOLDER, 'zip', ".", HARNESS_FOLDER)

        # time safety check
        time_pattern = re.compile("^[0-9]+[smhd]+$")
        if (not time_pattern.match(args.fuzz_time)):
            parser.print_help()
        else:
            if args.action == "fuzz_signature":
                fuzz_signature(args.target, args.fuzz_time, args.from_stdin, args.parallel_fuzzing)
            else:
                fuzz_one(args.target, args.fuzz_time, args.from_stdin, args.parallel_fuzzing)

        # remove zip folder
        os.remove(HARNESS_FOLDER_ZIP)

    elif args.action == "check":
        check()
    elif args.action == "kill_fuzzer":
        kill_fuzzer(args.target)
    else:
        print(3)
        parser.print_help()


