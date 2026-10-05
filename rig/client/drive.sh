#!/bin/sh
# usage: drive.sh <hack ...>  Drives the running meshac-wurst client: toggles each Wurst hack via chat, walks forward, counts SIGNAL lines.
X="docker exec -e DISPLAY=:99 meshac-wurst xdotool"
say() { $X key --delay 80 t; sleep 0.6; $X type --delay 40 "$1"; $X key Return; sleep 0.6; }
for h in "$@"; do
  t0=$(date -u +%Y-%m-%dT%H:%M:%S)
  say ".t $h"
  $X keydown w; $X keydown space; sleep 8; $X keyup space; sleep 4; $X keyup w
  say ".t $h"; sleep 2
  echo "RESULT $h signals=$(docker logs --since $t0 meshac-rig 2>&1 | grep -c "SIGNAL w_\|SIGNAL wtest")"
  docker logs --since $t0 meshac-rig 2>&1 | grep "SIGNAL" | sed 's/.*SIGNAL [^ ]* //' | cut -c1-24 | sort | uniq -c | sort -rn | head -3
done
