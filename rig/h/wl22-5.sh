#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h/
. ./run.sh
RES=/tmp/wl22-5.csv
: > "$RES"
NAME=mcWl25
CASES="n8c"
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p /home/luke/meshac-work/clients/wurst-263/run/wurst
 echo [] > /home/luke/meshac-work/clients/wurst-263/run/wurst/enabled-hacks.json
 (cd /home/luke/meshac-work/meshac/rig/client && ./run-client-lag.sh /home/luke/meshac-work/clients/wurst-263 /home/luke/meshac-work/clients/gh $NAME >/dev/null)
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}
ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
sh ./world.sh >/dev/null; rc "forceload add -16 -16 16 16" >/dev/null; rc "fill -12 -61 -12 12 -61 12 stone" >/dev/null
start_client || exit 1
setup_minefix(){ setup_mine;rc 'fill 96 -61 58 104 -61 62 stone' >/dev/null;PRE='X key 1; X mousedown 1; (sleep 10; X mouseup 1) &'; }
measure_minefix(){ measure_mine; }
setup_minefix(){ setup_mine;rc "fill 96 -61 58 104 -61 62 stone" >/dev/null;PRE="X key 1; X mousedown 1; (sleep 10; X mouseup 1) &"; }
measure_minefix(){ measure_mine; }
for c in $CASES; do
 docker stop meshac-wurst >/dev/null 2>&1; start_client || exit 1
 t0=$(date -u +%Y-%m-%dT%H:%M:%S)
 run_case $c none
 docker logs --since $t0 meshac-rig 2>&1 | grep -E "trace\] (sweep|break) $NAME|SIGNAL $NAME" > /tmp/wl22-5-$c.txt
 echo "## $c breaks=$(grep -c 'trace\] break' /tmp/wl22-5-$c.txt) instant=$(grep -c 'instant=true' /tmp/wl22-5-$c.txt) max=$(grep 'trace\] sweep' /tmp/wl22-5-$c.txt | sed 's/.*sweep=//' | sort -n | tail -1) signals=$(grep -c SIGNAL /tmp/wl22-5-$c.txt)" >> /tmp/wl22-5.csv
 rc "mesh unban $NAME" >/dev/null
done
echo FIN >> /tmp/wl22-5.csv
