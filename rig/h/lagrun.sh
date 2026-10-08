#!/bin/bash
# usage: lagrun.sh <stall_ms> <every_s> <tag>   N8 flicker through the lag proxy against the 26.3 rig
H=/home/luke/meshac-work/meshac/rig/h
docker rm -f meshac-rig meshac-wurst meshac-lagproxy >/dev/null 2>&1
MESHAC_TRACE=1 sh /home/luke/meshac-work/meshac/rig/run-server.sh /home/luke/meshac-work/rig/smoke-26.3 >/dev/null; sleep 30
docker run -d --name meshac-lagproxy --network meshac-net -e STALL_MS=$1 -e EVERY_S=$2 -v $H/lagproxy.py:/p.py python:3-alpine python /p.py >/dev/null
sed "s/NAME=mcWl22/NAME=mcWl2$3/; s#/tmp/wl22#/tmp/wl22-$3#g" $H/wl22.sh > $H/wl22-$3.sh
bash $H/wl22-$3.sh
docker rm -f meshac-rig meshac-wurst meshac-lagproxy >/dev/null 2>&1
