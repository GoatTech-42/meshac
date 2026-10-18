cd ~/meshac-work/meshac/rig/h
. ./run.sh
NAME=meshRigLegit26
RES=/tmp/meteor-legit-terrain.csv
ensure(){ connected && [ "$(hp)" != 0.0 ]; }
say ".t all off"
for c in "$@"; do run_case "$c" none; done
