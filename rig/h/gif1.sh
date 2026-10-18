#!/bin/bash
# GIF 1: Wurst Flight on the rig with /mesh watch on. Records frames from the client display.
cd /home/luke/meshac-work/meshac/rig/h/
. ./lib.sh
NAME=mcGif$(date +%s); TAG=${1:-fly}
trap "docker stop meshac-wurst >/dev/null 2>&1" EXIT
mkdir -p /tmp/gif-$TAG; rm -f /tmp/gif-$TAG/*
sh ./world.sh >/dev/null
reset_hacks; docker rm -f $CL >/dev/null 2>&1
(cd /home/luke/meshac-work/meshac/rig/client && ./run-client.sh /home/luke/meshac-work/clients/wurst /home/luke/meshac-work/clients/gh $NAME >/dev/null)
for i in $(seq 1 45); do sleep 4; connected && [ -n "$(hp)" ] && break; done; sleep 10
rc "time set day" >/dev/null; rc "gamerule advance_time false" >/dev/null; rc "op $NAME" >/dev/null; rc "gamemode survival $NAME" >/dev/null; rc "effect clear $NAME" >/dev/null
rc "tp $NAME 100.5 -60 0.5 0 15" >/dev/null; sleep 3
docker exec $CL mkdir -p /tmp/fr
( n=0; while true; do docker exec -e DISPLAY=:99 $CL import -window root -resize 854x480 /tmp/fr/f$(printf %04d $n).png 2>/dev/null; n=$((n+1)); done ) &
REC=$!
sleep 2
say "/mesh watch"; sleep 2
say ".t Flight"; sleep 1
X keydown space; sleep 0.07; X keyup space; sleep 0.12; X keydown space; sleep 0.07; X keyup space
X keydown w
for i in $(seq 1 25); do sleep 1; connected || break; done
X keyup w
sleep 5
kill $REC 2>/dev/null; sleep 1
docker cp $CL:/tmp/fr/. /tmp/gif-$TAG/ >/dev/null 2>&1
ls /tmp/gif-$TAG | wc -l
docker logs meshac-rig 2>&1 | grep "SIGNAL $NAME\|kick\|Kicked" | tail -6 | cut -c1-200
