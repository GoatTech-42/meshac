D="$(dirname "$0")"; . "$D/run.sh"; NAME=mc$(date +%H%M%S); RES=/tmp/dbg.csv
start_client || exit 1; sh "$D/world.sh" >/dev/null
for c in "$@"; do run_case $c; done; echo DONE
