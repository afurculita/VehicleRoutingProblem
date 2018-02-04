#!/bin/bash
###############################################################################
# This script is run for each candidate to evaluate it after all
# candidate configurations have been run on a single instance.
#
# Check the examples in examples/
#
# PARAMETERS:
# $1 is the candidate configuration number
# $2 is the instance id
# $3 is the seed
# $4 is the instance name
# $5 is the number of candidates alive in this iteration
#
# ONLY FOR ELITIST RACE: The rest ($* after `shift 5') are the ids of the 
# candidates alive in this iteration. This list can be used to calculate the 
# hypervolume using previous execution results.
#
# RETURN VALUE:
# This script should print one numerical value: the cost that must be minimized.
# Exit with 0 if no error, with 1 in case of error
###############################################################################
CANDIDATE="$1"
INSTANCEID="$2"
SEED="$3"
INSTANCE="$4"
TOTALCANDIDATES="$5"
shift 5 || error "Not enough parameters to $0"
ALLIDS=$*

STDOUT=c${CANDIDATE}-${INSTANCEID}.stdout
STDERR=c${CANDIDATE}-${INSTANCEID}.stderr

error() {
    echo "`TZ=UTC date`: error: $@"
    exit 1
}

# # This may be used to introduce a delay if there are filesystem
# # issues.
# SLEEPTIME=1
# while [ ! -s "${STDOUT}" ]; do
#     sleep $SLEEPTIME
#     let "SLEEPTIME += 1"
# done

# This is an example of reading a number from the output of
# target-runner. It assumes that the objective value is the first number in
# the first column of the only line starting with a digit.
if [ -s "${STDOUT}" ]; then
    COST=$(cat ${STDOUT} | grep -e '^[[:space:]]*[+-]\?[0-9]' | cut -f1)
    echo "$COST"
    rm -f "${STDOUT}" "${STDERR}"
    exit 0
else
    error "${STDOUT}: No such file or directory"
fi
