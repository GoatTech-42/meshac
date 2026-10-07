cd ~/meshac-work/meshac/rig/h
. ./run.sh
NAME=meshRig26
RES=/tmp/meteor-kb.csv
: > "$RES"
say ".t all off"
run_case kbl none
run_case kbl velocity
