#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h/
. ./run.sh
RES=/tmp/open79.csv
: > "$RES"
NAME=mcOpen79
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
setup_buildhand(){ X0=100 Y0=-60 Z0=80; PITCH=70; SECS=6; CLICK=3; rc "fill 96 -60 76 104 -54 88 air" >/dev/null; rc "fill 96 -61 76 104 -61 88 stone" >/dev/null; rc "item replace entity $NAME hotbar.0 with cobblestone 64" >/dev/null; PRE="X key 1"; }
measure_buildhand(){ rc "data get entity $NAME Inventory"; }
setup_fastplacehand(){ setup_buildhand;rc "fill 90 -61 70 110 -61 100 stone" >/dev/null;KEYS="s";PITCH=75;SECS=8;CLICK=0;PRE="X key 1;X mousedown 3;(sleep 8;X mouseup 3) &"; }
measure_fastplacehand(){ measure_buildhand; }
run_case fastplacehand none
docker logs -t meshac-rig > /tmp/open79-server.log 2>&1
echo FIN >> /tmp/open79.csv
