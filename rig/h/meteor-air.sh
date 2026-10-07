cd ~/meshac-work/meshac/rig/h
. ./run.sh
NAME=meshRig26
setup_airjump() { X0=100 Y0=-60 Z0=940; KEYS=''; SECS=12; rc 'forceload add 100 940' >/dev/null; rc 'fill 94 -60 934 106 -30 946 air' >/dev/null; rc 'fill 94 -61 934 106 -61 946 stone' >/dev/null; PRE='(for i in $(seq 1 35); do X key space; sleep 0.25; done) &'; }
measure_airjump() { pos | cut -d, -f2; }
RES=/tmp/meteor-air.csv
: > "$RES"
say '.t all off'
run_case airjump none
run_case airjump air-jump
