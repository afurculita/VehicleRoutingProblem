#!/usr/bin/python
###############################################################################
# This script is the command that is executed every run.
# Check the examples in examples/
#
# This script is run in the execution directory (execDir, --exec-dir).
#
# PARAMETERS:
# argv[1] is the candidate configuration number
# argv[2] is the instance ID
# argv[3] is the seed
# argv[4] is the instance name
# The rest (argv[5:]) are parameters to the run
#
# RETURN VALUE:
# This script should print one numerical value: the cost that must be minimized.
# Exit with 0 if no error, with 1 in case of error
###############################################################################

import datetime
import os.path
import re
import subprocess
import sys

exe = "~/bin/executable"
fixed_params = " "

if len(sys.argv) < 5:
    print "\nUsage: ./target-runner.py <candidate_id> <instance_id> <seed> <instance_path_name> <list of parameters>\n"
    sys.exit(1)

def target_runner_error(msg):
    now = datetime.datetime.now()
    print(str(now) + " error: " + msg)
    sys.exit(1)

# Get the parameters as command line arguments.
candidate_id = sys.argv[1]
instance_id = sys.argv[2]
seed = sys.argv[3]
instance = sys.argv[4]
cand_params = sys.argv[5:]

# Define the stdout and stderr files.
out_file = "c" + str(candidate_id) + "-" + str(instance_id) + ".stdout"
err_file = "c" + str(candidate_id) + "-" + str(instance_id) + ".stderr"

if not os.path.isfile(exe):
    target_runner_error (str(exe) + " not found")
if not os.access(exe, os.X_OK):
    now = datetime.datetime.now()
    print(str(now) + " error: " + str(exe) + " is not executable")

# Build the command, run it and save the output to a file,
# to parse the result from it.
# 
# Stdout and stderr files have to be opened before the call().
#
# Exit with error if something went wrong in the execution.

command = [exe] + fixed_params.split() + ["-i"] + [instance] + ["--seed"] + [seed] + cand_params

outf = open(out_file, "w")
errf = open(err_file, "w")
return_code = subprocess.call(command, stdout = outf, stderr = errf)
outf.close()
errf.close()

if return_code != 0:
    now = datetime.datetime.now()
    print(str(now) + " error: command returned code " + str(return_code))
    sys.exit(1)

if not os.path.isfile(out_file):
    now = datetime.datetime.now()
    print(str(now) + " error: output file "+ out_file  +" not found.")
    sys.exit(1)
# This is an example of reading a number from the output.
# It assumes that the objective value is the first number in
# the first column of the last line of the output.

lastline = [line.rstrip('\n') for line in open(out_file)][-1]

# from http://stackoverflow.com/questions/4703390
numeric_const_pattern = r"""
     [-+]? # optional sign
     (?:
         (?: \d* \. \d+ ) # .1 .12 .123 etc 9.1 etc 98.1 etc
         |
         (?: \d+ \.? ) # 1. 12. 123. etc 1 12 123 etc
     )
     # followed by optional exponent part if desired
     (?: [Ee] [+-]? \d+ ) ?
     """
rx = re.compile(numeric_const_pattern, re.VERBOSE)

cost = rx.findall(lastline)[0]
print(cost)

os.remove(out_file)
os.remove(err_file)

sys.exit(0)
