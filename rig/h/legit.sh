#!/bin/sh
# Standing legit-player suite. Every scenario runs with NO hack and must end connected with 0 signals.
# usage: sh legit.sh [scenario ...]   Exit 1 if anything flagged.
D="$(dirname "$0")"; . "$D/run.sh"; NAME=mc$(date +%H%M%S); RES=/tmp/legit.csv; : > $RES
LIST=${*:-"walk fall water wall ladder step web soulsand snow boat push meleeclick meleetank mine build chest food levitate zombiehit swapback fastplace scaffold totemf totemz elytra anchorl"}
start_client || exit 1; sh "$D/world.sh" >/dev/null
for c in $LIST; do run_case $c none >/dev/null; done
sed 's/"[^"]*"/Q/g' $RES > $RES.flat
awk -F, '{ if ($NF!="connected" || $7!=0) print "FLAG  " $1 " signals=" $7 " top=" $11 " " $NF; else print "clean " $1 }' $RES.flat
bad=$(awk -F, '$NF!="connected" || $7!=0' $RES.flat | wc -l); echo "legit suite: $bad flagged of $(wc -l < $RES.flat)"; [ "$bad" = 0 ]
