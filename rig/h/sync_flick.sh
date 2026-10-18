#!/bin/bash
# usage: sync_flick.sh <player> <minpx> <maxpx> <secs>   Holds left, and after each break the server logs for <player>, waits 200 ms and flicks the view.
N=$1; LO=$2; HI=$3; S=${4:-24}
X() { docker exec -e DISPLAY=:99 meshac-wurst xdotool "$@"; }
X key 1; X mousedown 1; X mousemove_relative -- 300 0
timeout $S bash -c "docker logs -f --since 1s meshac-rig 2>&1 | grep --line-buffered \"trace\\] break $N\"" | while read -r l; do
  echo "$(date +%T.%N) trig" >> /tmp/sf.log; sleep 0.2; d=$(shuf -i $LO-$HI -n1); [ $((RANDOM%2)) = 0 ] && d=-$d; X mousemove_relative -- $d 0
done
echo "$(date +%T) end" >> /tmp/sf.log; X mouseup 1
