#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
export NAME=mcLadG$(date +%s)
./gif3.sh r1 > /tmp/gif3-r1.out 2>&1
sleep 5
./gif3.sh r2 > /tmp/gif3-r2.out 2>&1
echo DONE >> /tmp/gif3-r2.out
