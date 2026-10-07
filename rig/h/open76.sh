#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h/
. ./run.sh
RES=/tmp/open76.csv
: > "$RES"
NAME=mcOpen76
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
setup_vein(){ setup_minefix;rc 'fill 96 -61 50 104 -55 57 coal_ore' >/dev/null;SECS=16;PRE='X key 1;X click 1'; }
measure_vein(){ rc "data get entity $NAME Inventory"; }
setup_veinhold(){ setup_minefix;rc 'fill 96 -61 50 104 -55 57 coal_ore' >/dev/null;SECS=16;PRE='X key 1; X mousedown 1; (sleep 14; X mouseup 1) &'; }
measure_veinhold(){ rc "data get entity $NAME Inventory"; }
#run_case veinhold none
for pair in 'vein VeinMiner'; do
 set -- $pair
 docker stop meshac-wurst >/dev/null
 rc "mesh unban $NAME" >/dev/null
 start_client || exit 1
 run_case "$1" "$2"
 docker logs -t meshac-rig > /tmp/open76-server.log 2>&1
done
docker logs -t meshac-rig > /tmp/open76-server.log 2>&1
echo FIN >> /tmp/open76.csv
