#!/bin/sh
# usage: drive.sh <hack ...>  Toggles each Wurst hack via chat, walks forward + jumps, reports meshac signals, time to first signal / hold / kick, and client-side vanilla outcome.
X="docker exec -e DISPLAY=:99 meshac-wurst xdotool"
say() { $X key --delay 80 t; sleep 0.6; $X type --delay 40 "$1"; $X key Return; sleep 0.6; }
for h in "$@"; do
  say ".t $h"
  t0=$(date +%s.%N); iso=$(date -u +%Y-%m-%dT%H:%M:%S)
  $X keydown w; $X keydown space; sleep 8; $X keyup space; sleep 4; $X keyup w
  say ".t $h"; sleep 1
  L=$(docker logs -t --since $iso meshac-rig 2>&1 | grep "SIGNAL wb_")
  n=$(echo "$L" | grep -c SIGNAL)
  first() { ts=$(echo "$L" | grep -m1 "$1" | cut -d" " -f1); [ -z "$ts" ] && { echo -; return; }; e=$(date -d "$ts" +%s.%N); awk -v a="$e" -v b="$t0" 'BEGIN{printf "%.1f", a-b}'; }
  echo "RESULT $h signals=$n first=$(first SIGNAL)s hold=$(first hold)s kick=$(first kick)s"
  echo "$L" | sed 's/.*SIGNAL [^ ]* //' | cut -c1-30 | sort | uniq -c | sort -rn | head -2
done
