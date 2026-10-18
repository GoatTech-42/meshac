#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/scaffold41.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcPlace38
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p /home/luke/meshac-work/clients/wurst-263/run/wurst
 echo [] > /home/luke/meshac-work/clients/wurst-263/run/wurst/enabled-hacks.json
 (cd /home/luke/meshac-work/meshac/rig/client && ./run-client.sh /home/luke/meshac-work/clients/wurst-263 /home/luke/meshac-work/clients/gh $NAME >/dev/null)
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}
ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
start_client || exit 1
sh ./world.sh >/dev/null


setup_bridge(){ X0=100 Y0=-41 Z0=800; KEYS='w'; SECS=12; PITCH=20; rc 'forceload add 90 790 110 870' >/dev/null; rc 'fill 96 -46 798 104 -38 866 air' >/dev/null; rc 'fill 98 -42 799 102 -42 801 stone' >/dev/null; rc "effect give $NAME resistance 60 255 true" >/dev/null; rc "item replace entity $NAME hotbar.0 with cobblestone 64" >/dev/null; PRE='X key 1'; }
measure_bridge(){ rc "data get entity $NAME Inventory"; }
run_case bridge none
rc "effect give $NAME instant_health 1 10 true" >/dev/null
run_case bridge ScaffoldWalk
cp /home/luke/meshac-work/clients/wurst-263/run/wurst/enabled-hacks.json /tmp/scaffold41-modules-after.json
