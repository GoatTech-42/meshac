#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h/
. ./run.sh
RES=/tmp/wl3.csv
: > "$RES"
NAME=mcWl05
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p /home/luke/meshac-work/clients/wurst-263/run/wurst
 echo [] > /home/luke/meshac-work/clients/wurst-263/run/wurst/enabled-hacks.json
 (cd /home/luke/meshac-work/meshac/rig/client && ./run-client.sh /home/luke/meshac-work/clients/wurst-263 /home/luke/meshac-work/clients/gh $NAME >/dev/null)
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}
ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
sh ./world.sh >/dev/null; rc "forceload add -16 -16 16 16" >/dev/null; rc "fill -12 -61 -12 12 -61 12 stone" >/dev/null
start_client || exit 1
setup_minefix(){ setup_mine;rc 'fill 96 -61 58 104 -61 62 stone' >/dev/null;PRE='X key 1; X mousedown 1; (sleep 10; X mouseup 1) &'; }
measure_minefix(){ measure_mine; }
setup_minefix(){ setup_mine;rc "fill 96 -61 58 104 -61 62 stone" >/dev/null;PRE="X key 1; X mousedown 1; (sleep 10; X mouseup 1) &"; }
measure_minefix(){ measure_mine; }
for pair in "nuke Nuker" "nuke none" "minefix none" ; do
 set -- $pair
 docker stop meshac-wurst >/dev/null 2>&1
 rc "mesh unban $NAME" >/dev/null
 start_client || exit 1
 run_case "$1" "$2"
 docker logs -t meshac-rig > /tmp/wl3-server.log 2>&1
done
echo FIN >> /tmp/wl3.csv
