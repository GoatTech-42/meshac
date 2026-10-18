#!/bin/sh
# usage: fullrun.sh <hacklist-file> <outcsv> <client-name>
# One client session for the whole list. Reconnects after kicks, resets hacks with .t panic, toggles each hack, walks+jumps 8 s, records meshac signals/hold/kick timing.
LIST=$1; OUT=$2; N=$3; START=$4
X="docker exec -e DISPLAY=:99 meshac-wurst xdotool"
say() { $X key --delay 80 t; sleep 0.7; $X type --delay 35 "$1"; $X key Return; sleep 0.7; }
joined() { docker logs meshac-rig 2>&1 | grep -c "$N joined the game"; }
left()   { docker logs meshac-rig 2>&1 | grep -c "$N left the game"; }
connected() { [ "$(joined)" -gt "$(left)" ]; }
ensure() {
  connected && return 0
  $X mousemove 640 465 click 1; sleep 10
  connected && return 0
  echo "[]" > ~/meshac-work/clients/wurst/run/wurst/enabled-hacks.json
  docker rm -f meshac-wurst >/dev/null 2>&1
  (cd ~/meshac-work/meshac/rig/client && ./run-client.sh ~/meshac-work/clients/wurst ~/meshac-work/clients/gh $N >/dev/null)
  for i in $(seq 1 40); do sleep 4; connected && { sleep 8; return 0; }; done
  return 1
}
mkdir -p /tmp/shots; [ -z "$START" ] && echo "hack,signals,first_s,hold_s,kick_s,top_signal,client_state" >> $OUT
echo "[]" > ~/meshac-work/clients/wurst/run/wurst/enabled-hacks.json
cd ~/meshac-work/meshac/rig/client
./run-client.sh ~/meshac-work/clients/wurst ~/meshac-work/clients/gh $N >/dev/null
for i in $(seq 1 40); do sleep 4; connected && break; done
sleep 8
for h in $(cat $LIST); do
  if ! ensure; then echo "$h,0,-,-,-,-,client lost (restart needed)" >> $OUT; continue; fi
  sleep 6; say ".t panic"; sleep 1
  iso=$(date -u +%Y-%m-%dT%H:%M:%S); t0=$(date +%s.%N)
  say ".t $h"
  $X keydown w; $X keydown space; sleep 4
  docker exec -e DISPLAY=:99 meshac-wurst import -window root /tmp/s.png 2>/dev/null && docker cp meshac-wurst:/tmp/s.png /tmp/shots/$h.png 2>/dev/null
  sleep 4; $X keyup space; sleep 2; $X keyup w
  state=connected; connected || state=kicked
  connected && say ".t $h"
  L=$(docker logs -t --since $iso meshac-rig 2>&1 | grep "SIGNAL $N")
  n=$(echo "$L" | grep -c SIGNAL)
  first() { ts=$(echo "$L" | grep -m1 "$1" | cut -d" " -f1); [ -z "$ts" ] && { echo -; return; }; e=$(date -d "$ts" +%s.%N); awk -v a="$e" -v b="$t0" 'BEGIN{printf "%.1f", a-b}'; }
  top=$(echo "$L" | sed 's/.*SIGNAL [^ ]* //' | awk '{print $1}' | sort | uniq -c | sort -rn | head -1 | awk '{print $2}')
  echo "$h,$n,$(first SIGNAL),$(first hold),$(first kick),${top:--},$state" >> $OUT
  sleep 10
done
echo DONE >> $OUT
