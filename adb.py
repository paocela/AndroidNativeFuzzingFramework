import subprocess
import time
import logging
import signal
import sys
import os
import string
import random
import traceback

log = logging.getLogger(__name__)


class DeviceUnresponsiveException(Exception):
    pass


class ADBSetTimeException(Exception):
    pass


def sig_unresponsive_handler(signum, frame):
    raise DeviceUnresponsiveException("Device does not respond.")


def subprocess_adb(cmd, device_id=None, wait_for_termination=True):
    if device_id:
        cmd.insert(0, device_id)
        cmd.insert(0, "-s")
    cmd.insert(0, "adb")
    if not wait_for_termination:
        cmd.insert(0, "15s") # this value could vary (depending on how much time the fuzzing_... script takes to start)
        cmd.insert(0, "timeout")
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=0)
    else:
        try:
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=0)
        except OSError as e:
            print(traceback.format_exc())
            log.error("Did you add adb to your $PATH?")
            sys.exit(1)
    return p


def call_adb(cmd, device_id=None, wait_for_termination=True, timeout=0):
    signal.signal(signal.SIGALRM, sig_unresponsive_handler)
    signal.alarm(timeout)
    p = subprocess_adb(cmd, device_id, wait_for_termination)
    try:
        out, err = p.communicate()
        p.wait()
    except DeviceUnresponsiveException as e:
        p.terminate()
        signal.alarm(0)
        raise DeviceUnresponsiveException("Device {} does not respond.".format(device_id))
    signal.alarm(0)
    return out, err


def cat_file(file_, device_id=None, has_eof=True):
    """ Cat a file on the device """
    if has_eof:
        out, err = execute_privileged_command("cat {}".format(file_), device_id)
    else:
        # we want to receive a SIGALRM when the non-eof file does not provide
        # us with data anymore. This signal is caught and we continue execution
        # under the assumption that there is not new data coming from the 
        # cat'ed file on the device.
        signal.signal(signal.SIGALRM, sig_unresponsive_handler)
        signal.alarm(5)

        out = b""
        p = subprocess_privileged("cat {}".format(file_), device_id)

        while True:
            line = b""
            try:
                line = p.stdout.readline()
                # reset the timer to 5 seconds
                signal.alarm(5)
            except DeviceUnresponsiveException as e:

                p.terminate()
                signal.alarm(0)

            if not line:
                break

            out += line

        err = p.stderr.read()
    return out, err


def set_date(device_id=None):
    """ sets date and timezone of device to values from host

        Remark: this function was written with 'toybox date' on Android in mind.
    """
    # get timezone from host
    if not os.path.exists("/etc/timezone"):
        raise NotImplementedError("Adapt to your host.")
    with open("/etc/timezone") as f:
        timezone = f.read().strip()
    if not timezone:
        raise NotImplementedError("Adapt to your host.")
    # set time on device
    current_time = time.localtime()
    formatted_time = time.strftime('%m%d%H%M%y.%S', current_time)
    # TODO: get TZ from HOST
    out, err = execute_privileged_command("TZ=CEST date {} && TZ=CEST date +%Y%m%d%H%M".format(formatted_time), device_id, 5)
    # check if we are in sync now
    out_lines = out.split(b"\n")
    if len(out_lines) < 2:
        raise NotImplementedError("This implementation needs to be tuned.")

    if time.strftime('%Y%m%d%H%M', current_time).encode() != out_lines[1].strip():
        raise ADBSetTimeException("Could not set time on device.")

    return out, err


def subprocess_privileged(cmd, device_id=None):
    """ Get subprocess of privileged command """
    p = subprocess_adb(["shell", "su -c '{}'".format(cmd)], device_id)
    time.sleep(2)
    if p.poll():
        p = subprocess_adb(["shell", "su root {}".format(cmd)], device_id)
    if p.poll():
        log.error("error creating privileged subprocess.")
        p = None
    return p


def execute_privileged_command(cmd, device_id=None, wait_for_termination=True, timeout=0):
    """ Executes the given privileged command on the device """
    out, err = call_adb(["shell", "su -c '{}'".format(cmd)], device_id, wait_for_termination, timeout)
    if err or b"invalid uid" in out:
        out, err = call_adb(["shell", "su root sh -c '{}'".format(cmd)], device_id, wait_for_termination, timeout)
    if err:
        log.error("error executing privileged cmd. err: {}".format(err))
    return out, err


def execute_command(cmd, device_id=None, timeout=0):
    """ Executes the given command on the device. If given, device_id is passed to adb -s option """
    out, err = call_adb(["shell", "{}".format(cmd)], device_id, timeout)
    return out, err


def remove_forward(device_id=None):
    """ Remove socket forwarding """
    out, err = call_adb(["forward", "--remove"], device_id)
    return out


def forward(lport, dport, device_id=None):
    """ Forward socket connection """
    out, err = call_adb(["forward", "tcp:{}".format(lport), "tcp:{}".format(dport)], device_id)
    return out


def list_devices():
    """ Lists connected devices """
    out, err = call_adb(["devices"])
    return out


def get_device_ids():
    """ Returns a list of device ids.

        Only device ids of devices that allow access are returned.
    """
    ids = []
    out = list_devices()
    lines = out.split(b"\n")
    for line in lines:
        tokens = line.split(b"\t")
        if len(tokens) == 2 and tokens[1] == b"device":
            ids.append(tokens[0])
    return ids


def get_logcat(device_id):
    """ Get logcat without filters """
    out, err = call_adb(["logcat", "-d"], device_id)
    return out


def push(what, where, device_id=None):
    """ Push to the device """
    out, err = call_adb(["push", what, where], device_id)
    return out
    
def push_privileged(what, where, is_directory=False, device_id=None):
    """ Push to the device """
    rand_str = list(string.ascii_letters)
    random.shuffle(rand_str)
    workdir = os.path.join("/data/local/tmp", "".join(rand_str[:10]))
    
    what_file = os.path.basename(what)
    workdir_file = os.path.join(workdir, what_file)
    
    execute_privileged_command(" ".join(["mkdir", workdir]), device_id)
    execute_privileged_command(" ".join(["chown", "shell:shell", workdir]), device_id)
    
    out, err = call_adb(["push", what, workdir], device_id)
    
    if is_directory:
    	execute_privileged_command(" ".join(["chown", "-R", "shell:shell", workdir_file]), device_id)
    	execute_privileged_command(" ".join(["cp", "-r", workdir_file, where]), device_id)
    else:
    	execute_privileged_command(" ".join(["chown", "shell:shell", workdir_file]), device_id)
    	execute_privileged_command(" ".join(["cp", workdir_file, where]), device_id)

    execute_privileged_command(" ".join(["rm", "-rf", workdir]), device_id)

    return out

def pull(what, where, device_id=None):
    """ Pull from the device """
    out, err = call_adb(["pull", what, where], device_id)
    return out


def pull_privileged(what, where, is_directory=False, device_id=None):
    """ Pull from the device """
    rand_str = list(string.ascii_letters)
    random.shuffle(rand_str)
    workdir = os.path.join("/data/local/tmp", "".join(rand_str[:10]))
    what_file = os.path.basename(what)
    workdir_file = os.path.join(workdir, what_file)

    execute_privileged_command(" ".join(["mkdir", workdir]), device_id)
    if is_directory:
    	execute_privileged_command(" ".join(["cp", "-r", what, workdir]), device_id)
    	execute_privileged_command(" ".join(["chown", "-R", "shell:shell", workdir_file]), device_id)
    else:
    	execute_privileged_command(" ".join(["cp", what, workdir]), device_id)
    	execute_privileged_command(" ".join(["chown", "shell:shell", workdir_file]), device_id)
    
    out, err = call_adb(["pull", workdir_file, where], device_id)
    execute_privileged_command(" ".join(["rm", "-rf", workdir]), device_id)

    return out


def reboot(device_id):
    """ Reboot the device """
    # on newer devices, `adb shell "reboot"` strangely takes a little longer 
    # to return
    out, err = call_adb(["reboot"], device_id, 20)
    return out


def reboot_recovery(device_id=None):
    """ Reboot device into recovery """
    out, err = call_adb(["reboot", "recovery"], device_id)
    return out


def wait_device_booted(device_id):
    """ check property init.svc.bootanim until its status is "stopped" and the system
        is fully booted """
    boot_status = ""
    nretry = 12
    while True:
        try:
            #out, _ = execute_command("getprop init.svc.bootanim", device_id, timeout=5)
            out, _ = execute_command("pwd", device_id, timeout=5)
            boot_status = out
        except DeviceUnresponsiveException:
            nretry -= 1
            if not nretry:
                raise

        if b"/" in boot_status:
            break

        if b"stopped" in boot_status:
            break

        if b"running" in boot_status:
            log.info("boot anim running...")
        else:
            nretry -= 1
            log.info("no response, retrying...")
            if nretry <= 0:
                raise DeviceUnresponsiveException("Device {} does not respond.".format(device_id))
        time.sleep(10)
    return True


def is_device_ready(device_id):
    """ Check if device is ready and pwd works.
    """
    device_ready = False
    if wait_device_booted(device_id):
        out, _ = execute_command('pwd', device_id, timeout=5)
        if out.strip() == b"/":
            device_ready = True
    return device_ready


def is_booted(device_id):
    """ Check if device properly booted and we can access `pwd`.
    """
    device_ready = False
    nretry = 20
    while True:
        try:
            out, _ = execute_command("pwd", device_id, timeout=5)
            if out.strip() == b"/":
                device_ready = True
                break
        except DeviceUnresponsiveException:
            nretry -= 1
            if not nretry:
                raise
        nretry -= 1
        log.info("no response, retrying...")
        if nretry <= 0:
            raise DeviceUnresponsiveException("Device {} does not respond.".format(device_id))
        time.sleep(10)
    return device_ready


def program_exists(program_name, device_id=None):
    """ Check if program exists"""
    out, _ = execute_command("which {}".format(program_name), device_id)
    if program_name.encode() in out:
        return True
    return False


def path_exists(path, device_id=None):
    """ Check if path exists."""
    out, err = execute_command("ls {}".format(path), device_id)

    if b"No such file" in err or b"No such file" in out:
        return False
    return True


def pgrep(process_name, device_id=None):
    """ Grep process by name """
    out = b""
    if program_exists("pgrep", device_id):
        tmp_out, _ = execute_privileged_command("pgrep {}".format(process_name), device_id)
        out += tmp_out
    else:
        raise NotImplementedError("Extend cmds to pgrep processes here.")
    return out


def pidof(process_name, device_id=None):
    """ Get pid of process by name """
    out = b""
    if program_exists("pidof", device_id):
        tmp_out, _ = execute_privileged_command(f"pidof {process_name}",
                                                device_id)
        out += tmp_out
    else:
        raise NotImplementedError("Extend cmds to pidof processes here.")
    return out


def pkill(process_name, device_id=None):
    """ Kill process by name """
    if program_exists("pkill", device_id):
        execute_privileged_command(f"pkill {process_name}", device_id)
    else:
        raise NotImplementedError("Extend cmds to pkill processes here.")


def kill(pid, device_id=None):
    """ Kill process by pid """
    if program_exists("kill", device_id):
        execute_privileged_command(f"kill -9 {pid}", device_id)
    else:
        raise NotImplementedError("Extend cmds to kill processes here.")
