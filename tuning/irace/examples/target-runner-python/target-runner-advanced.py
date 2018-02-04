#!/usr/bin/env python

##############################################################################
#                                                                            #
# With this target-runner, you can:                                          #
#                                                                            #
# - set environment variables                                                #
# - run your jobs read the standard output / standard err, and check the     #
#   return code                                                              #
# - you can choose between 5 different ways of running your job and reading  #
#   the standard out (you might have to try several ones until you find one  #
#   that is robust enough for your experiments); among the 5 different ways  #
#   there are various combinations of spawining subprocesses, using pipes or #
#   temporary files, and different methods to set a timeout after which the  #
#   job you are running is killed                                            #
# - you can choose how many times a job should be run before giving up (if   #
#   you have some heisenbugs, irace will not crash! :) but it will give the  #
#   configuration further chances)                                           #
# - if the job fails for several times in a row, it writes in your execution #
#   directory a c${configuration}.stdout and stderr as the normal            #
#   target-runner                                                            #
# - set a debug level, and have a detailed output to track what happens (one #
#   file per target-runner, use sparely)                                     #
#                                                                            #
##############################################################################


# ---------------------------- DO NOT CHANGE HERE ----------------------------
#                     (unless you know what you are doing)
# ---------------------- GO TO THE BOTTOM OF THIS FILE -----------------------


import os
import sys
import time
import socket
import logging
import tempfile
import subprocess
import threading


class Runner(object):

    def __init__(self, executable, fixed_params, instanceid, instance, seed,
                 parse_output, candidate, parameters, max_tests):
        self.executable = executable
        self.fixed_params = fixed_params
        self.instanceid = instanceid
        self.instance = instance
        self.seed = seed
        self.parse_output = parse_output
        self.candidate = candidate
        self.parameters = parameters
        self.max_tests = max_tests

        # default exec function
        self.execute = self.execute1

        self.maximize = False

        # logging (by default only errors are logged)
        filename = 'c' + self.candidate + '-' + self.seed + '-' + str(instanceid) + '.' + socket.gethostname() + '_' + str(os.getpid())
        self.logger = logging.getLogger('target-runner')
        try :
            hdlr = logging.FileHandler(filename, delay=True)
        except OSError as e:
            print "Current working dir : %s" % os.getenv('PWD')
            raise
        formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
        hdlr.setFormatter(formatter)
        self.logger.addHandler(hdlr)
        self.logger.setLevel(logging.ERROR)


    # when returning the cost multiply by -1 if maximizing
    def set_maximize(self):
        self.maximize = True


    # changes the log level
    def log_level(self, level):
        self.logger.setLevel(level)


    # changes the way the child process is executed
    def exec_mode(self, mode, max_time=3600):
        self.execute = mode
        # self.max_time is used only in execute_timeout functions
        self.max_time = max_time


    # executes a process, waits until it finishes
    # returns the exit code, as well as the stdout and stderr
    # this version uses no intermediate file, it reads directly from pipes, it
    # is less robust though, if the child gets oddly killed it seems to hang
    # even if it should not
    def execute1(self, command):
        self.logger.debug('job started')
        self.logger.debug('PATH=' + os.environ['PATH'])
        self.logger.debug(command)
        process = subprocess.Popen(command, stdout=subprocess.PIPE,
                                            stderr=subprocess.PIPE,
                                            env=os.environ)
        (out, err) = process.communicate()
        status = process.wait()
        self.logger.debug(out)
        self.logger.debug(err)
        self.logger.debug('job finished')
        return (status, out, err)


    # executes a process, waits until it finishes
    # returns the exit code, as well as the stdout and stderr
    def execute2(self, command):
        fout = tempfile.NamedTemporaryFile(delete=False)
        fout.close()
        ferr = tempfile.NamedTemporaryFile(delete=False)
        ferr.close()
        str_cmd = ' '.join(command) + ' >' + fout.name + ' 2>' + ferr.name
        try:
            self.logger.debug('job started')
            self.logger.debug('PATH=' + os.environ['PATH'])
            self.logger.debug(str_cmd)
            process = subprocess.check_call(str_cmd, shell=True)
            status = 0
        except subprocess.CalledProcessError as e:
            status = e.returncode
            self.logger.warning('Exit status: ' + str(status))
        out = self.read(fout.name)
        err = self.read(ferr.name)
        os.unlink(fout.name)
        os.unlink(ferr.name)
        self.logger.debug(out)
        self.logger.debug(err)
        self.logger.debug('job finished')
        return (status, out, err)


    # executes a process, waits until it finishes
    # returns the exit code, as well as the stdout and stderr
    # if the process does not finish before the timeout it gets killed
    def execute_timeout1(self, command):
        fout = tempfile.SpooledTemporaryFile()
        ferr = tempfile.SpooledTemporaryFile()
        start_time = time.time()
        self.logger.debug('job started')
        self.logger.debug('PATH=' + os.environ['PATH'])
        self.logger.debug(command)
        process = subprocess.Popen(command, stdout=fout, stderr=ferr,
                                   env=os.environ)
        elapsed_time = time.time() - start_time
        while process.poll() is None and elapsed_time <= self.max_time:
            time.sleep(1)
            elapsed_time = time.time() - start_time
            self.logger.debug('elapsed time: ' + str(elapsed_time))
        if elapsed_time > self.max_time:
            process.kill()
            self.logger.warning('job killed, ' + str(self.max_time) + \
                                ' seconds of wall-clock time elapsed')
            status = 1
        status = process.poll()
        fout.seek(0)
        ferr.seek(0)
        out = str(fout.read())
        err = str(ferr.read())
        fout.close()
        ferr.close()
        self.logger.debug(out)
        self.logger.debug(err)
        self.logger.debug('job finished')
        return (status, out, err)


    # executes a process, waits until it finishes
    # returns the exit code, as well as the stdout and stderr
    # this version is like execute2 but requires python3.3 to set the timeouts
    # for the Popen.communicate() and Popen.wait()
    # if the process does not finish before the timeout it gets killed
    def execute_timeout2(self, command):
        self.logger.debug('job started')
        self.logger.debug('PATH=' + os.environ['PATH'])
        self.logger.debug(command)
        process = subprocess.Popen(command, stdout=subprocess.PIPE,
                                            stderr=subprocess.PIPE,
                                            env=os.environ)
        try:
            (out, err) = process.communicate(timeout=self.max_time)
            status = process.wait(timeout=self.max_time)
        except subprocess.TimeoutExpired:
            process.kill()
            (out, err) = process.communicate()
            status = 1
            self.logger.warning('ob killed, ' + str(self.max_time) + \
                                ' seconds of CPU time elapsed')
        self.logger.debug(out)
        self.logger.debug(err)
        self.logger.debug('job finished')
        return (status, out, err)


    # executes a process, waits until it finishes (most robust one)
    # returns the exit code, as well as the stdout and stderr
    def execute_threaded_timeout(self, command):

        class ThreadedJob(threading.Thread):

            def __init__(self, command, logger):
                threading.Thread.__init__(self)
                self.shell_command = ' '.join(command)
                self.process = None
                self.out = None
                self.err = None
                self.logger = logger

            def run(self):
                self.logger.debug('job started')
                self.logger.debug('PATH=' + os.environ['PATH'])
                self.logger.debug(self.shell_command)
                self.process = subprocess.Popen(self.shell_command,
                                                stdout=subprocess.PIPE,
                                                stderr=subprocess.PIPE,
                                                shell=True,
                                                env=os.environ)
                self.out, self.err = self.process.communicate()
                self.logger.debug(self.out)
                self.logger.debug(self.err)
                self.logger.debug('job finished')

        self.logger.debug('creating thread')
        thread = ThreadedJob(command, self.logger)
        # make daemon so that when the main program exits all (eventually
        # hanging) threads are killed
        thread.setDaemon(True)
        thread.start()
        thread.join(timeout=self.max_time)
        self.logger.debug('joined (timeout ' + str(self.max_time) + ')')
        if thread.is_alive():
            # this part here is delicate we keep it in a try catch block
            try:
                self.logger.debug('thread is still alive, we timed out')
                # send a SIGTERM
                self.logger.debug('sending SIGTERM signal')
                thread.process.terminate()
                thread.join(timeout=5.0)
                self.logger.debug('done.')
                # send a SIGKILL to be very sure
                self.logger.debug('sending SIGKILL signal')
                thread.process.kill()
                thread.join(timeout=5.0)
                self.logger.debug('done')
            except Exception as e:
                self.logger.warning('exception when killing job: \n' + \
                                    str(e))
        status = thread.process.returncode
        out = str(thread.out)
        err = str(thread.err)
        # to be very sure
        del thread
        self.logger.debug('thread deleted, returning results')
        return (status, out, err)


    # reads and exports the environment variables
    def source_env(self, filename):
        self.logger.debug('setting environment variables')
        command = '"source ' + filename + ' >/dev/null 2>&1 && env"'
        (_, out, _) =  self.execute(['bash', '-c', command])
        lines = out.split('\n')
        for line in lines:
            (key, _, value) = line.partition('=')
            os.environ[key] = value
            self.logger.debug('setting ' + key + '=' + value)
        self.logger.debug('now PATH is: ' + os.environ['PATH'])


    # reads data into a string
    def read(self, filename):
        f = open(filename)
        content = str(f.read())
        f.close()
        return content


    # writes a string to file
    def save(self, filename, content):
        f = open(filename, 'w')
        f.write(str(content))
        f.close()


    # executing the program
    def run(self):
        test = 0
        cost = None
        while test < self.max_tests:
            command_list = [self.executable] + self.fixed_params.split() + \
                           [self.instance] + self.parameters
            (status, out, err) = self.execute(command_list)
            if status != 0:
                test += 1
                self.logger.warning('non-zero exit status *RETRYING* ' + \
                                    str(test) + ' of ' + str(self.max_tests))
                continue
            # parsing the output
            cost = self.parse_output(out)
            try:
                check = float(cost)
            except:
                test += 1
                self.logger.warning('cost was not a number *RETRYING* ' + \
                                    str(test) + ' of ' + str(self.max_tests))
                continue

            # convert to float and multiply by -1 if maximizing
            cost = float(cost)
            if self.maximize:
                cost *= -1

            break

        # printing the result
        if test < self.max_tests:
            self.logger.debug('returning cost: ' + str(cost))
            sys.stdout.write(str(cost) + '\n')
            # force to exit all possible threads except the main one (those
            # launched with execute_threaded_timeout) are run as daemons so
            # they should be terminated automatically when exiting, but just
            # to be extra sure
            sys.exit(0)
        else:
            # in case somehting goes wrong we write stdout and stderr files
            self.logger.error('something went wrong after ' + \
                              str(self.max_tests) + ' runs')
            self.logger.error('saving candidate stdout to ' + \
                                  'c' + self.candidate + '-' + self.seed + '-' + str(instanceid) + '.stdout')
            self.save('c' + self.candidate + '-' + self.seed + '-' + str(instanceid) + '.stdout', out)
            self.save('c' + self.candidate + '-' + self.seed + '-' + str(instanceid) + '.stderr', err)
            self.logger.error('returning cost: ' + str(cost))
            self.logger.error('exit status is ' + str(status))
            sys.stdout.write('something went wrong for candidate ' + \
                             self.candidate + '\n')
            if status != 0:
                sys.stdout.write('exit status: ' + str(status) + '\n')
                sys.exit(status)
            else:
                sys.stdout.write('could not cast to float the result: \'' + \
                                 str(cost) + '\n')
                sys.exit(1)


# ------------------------------ CHANGE HERE! ------------------------------ #


# parse here directly the stdout of your job (the 'out' parameter)
# alternatively you can ignore it and read other files produced by
# your job
def parse_output(out):
    # parsing last thing printed
    return out.strip().split()[-1]

def is_exe(fpath):
    return os.path.isfile(fpath) and os.access(fpath, os.X_OK) \
        and os.path.getsize(fpath) > 0

if __name__=='__main__':

    bindir = os.path.dirname(os.path.realpath(__file__))
    
    # reading parameters and setting problem specific stuff
    rootdir = bindir + "/../../../"
    timeout = 180
    candidate = sys.argv[1]
    instanceid = sys.argv[2]
    seed = sys.argv[3]
    instance = sys.argv[4]
    parameters = sys.argv[5:]

    executable = './' + candidate
    fixed_params = 'grammars/PFSPWCT.xml None 20 0 ' + seed

    # maximum number of trials before giving up with the configuration
    max_tests = 100
    hr = Runner(executable, fixed_params, instanceid, instance, seed,
                parse_output, candidate, parameters, max_tests)

    # maximizing instead of minimizing
    # hr.set_maximize()

    # write debug information (1 log per target-runner use sparely)
    # hr.log_level(logging.DEBUG)

    # execute through pipes (this is the default)
    # hr.exec_mode(hr.execute1)

    # execute through temporary files (slightly more robust)
    # hr.exec_mode(hr.execute2)

    # execute through temporary files with timeout
    # after 5 minutes of *wallclock time* if we do not get the results we kill
    # the subprocess and try another time...
    # hr.exec_mode(hr.execute_timeout1, 300)

    # python3 execute through pipes with timeout (slightly less robust)
    # hr.exec_mode(hr.execute_timeout2, 300)

    # execute through temporary files with timeout
    # after 2 minutes of *CPU time* if we do not get the results we kill
    # the subprocess and try another time...
    hr.exec_mode(hr.execute_threaded_timeout, timeout)

    # environment variables that should be set for testing each configuration
    hr.source_env(rootdir + 'configuration')

    # run the target-runner
    hr.run()


# -------------------------------------------------------------------------- #
