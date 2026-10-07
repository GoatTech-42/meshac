cd ~/meshac-work/meshac/rig/h
. ./run.sh
NAME=meshRig26
RES=/tmp/meteor-zkb.csv
: > "$RES"
say ".t all off"
setup_zkb(){ setup_zombiehit; rc "forceload add 100 700" >/dev/null; rc "fill 93 -60 693 107 -50 707 air" >/dev/null; rc "fill 93 -61 693 107 -61 707 stone" >/dev/null; }
measure_zkb(){ hp; }
run_case zkb none
run_case zkb velocity
