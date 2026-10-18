#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
export NAME=mcLadM$(date +%s)
./gif3.sh m1 > /tmp/gif5-l1.out 2>&1; sleep 4
./gif3.sh m2 > /tmp/gif5-l2.out 2>&1; sleep 4
echo DONE > /tmp/gif5.done
