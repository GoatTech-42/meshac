#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
export NAME=mcLadH$(date +%s)
./gif3.sh l1 > /tmp/gif4-l1.out 2>&1; sleep 4
./gif3.sh l2 > /tmp/gif4-l2.out 2>&1; sleep 4
./gif3.sh l3 > /tmp/gif4-l3.out 2>&1
echo DONE > /tmp/gif4.done
