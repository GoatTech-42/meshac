# Generic scenario runner. Each scenario defines setup_<name> (rcon world/inventory setup, sets X0 Y0 Z0 KEYS SECS CLICK)
# and optionally measure_<name> (prints one extra field describing whether the hack did its job).
# usage: . run.sh; run_case <scenario> <hack|none>
. "$(dirname "$0")/lib.sh"
. "$(dirname "$0")/scenarios.sh"
RES=${RES:-$HOME/meshac-work/results.csv}
run_case() {
  S=$1; H=$2; KEYS=""; SECS=8; CLICK=0; YAW=0; PITCH=0
  ensure || { echo "$S,$H,,,,,,,,,client lost" >> $RES; return; }
  rc "difficulty easy" >/dev/null; rc "time set midnight" >/dev/null; rc "gamerule spawn_mobs false" >/dev/null; rc "gamerule natural_health_regeneration false" >/dev/null
  rc "gamemode survival $NAME" >/dev/null; rc "effect clear $NAME" >/dev/null; rc "clear $NAME" >/dev/null
  rc "kill @e[type=!player]" >/dev/null; rc "effect give $NAME instant_health 1 10 true" >/dev/null
  setup_$S
  rc "tp $NAME $X0.5 $Y0 $Z0.5 $YAW $PITCH" >/dev/null; sleep 2
  p0=$(pos); h0=$(hp); m0=$(measure_$S 2>/dev/null)
  [ "$H" != none ] && say ".t $H"
  iso=$(date -u +%Y-%m-%dT%H:%M:%S); t0=$(date +%s.%N)
  for k in $KEYS; do X keydown $k; done
  i=0; while [ $i -lt $SECS ]; do [ $CLICK != 0 ] && X click $CLICK; sleep 1; i=$((i+1)); [ $i = $((SECS/2)) ] && { docker exec -e DISPLAY=:99 $CL import -window root /tmp/s.png 2>/dev/null; mkdir -p ~/meshac-work/shots; docker cp $CL:/tmp/s.png ~/meshac-work/shots/$S-$H.png 2>/dev/null; }; done
  for k in $KEYS; do X keyup $k; done; sleep 2
  st=connected; connected || st=kicked
  p1=$(pos); h1=$(hp); m1=$(measure_$S 2>/dev/null)
  [ $st = connected ] && [ "$H" != none ] && say ".t $H"
  L=$(docker logs -t --since $iso $RIG 2>&1 | grep "SIGNAL $NAME")
  n=$(echo "$L" | grep -c SIGNAL)
  first() { ts=$(echo "$L" | grep -m1 "$1" | cut -d" " -f1); [ -z "$ts" ] && { echo -; return; }; awk -v a="$(date -d "$ts" +%s.%N)" -v b="$t0" 'BEGIN{printf "%.1f", a-b}'; }
  top=$(echo "$L" | sed 's/.*SIGNAL [^ ]* //' | awk '{print $1}' | sort | uniq -c | sort -rn | head -1 | awk '{print $2}')
  echo "$S,$H,\"$p0\",\"$p1\",$h0>$h1,\"$m0>$m1\",$n,$(first SIGNAL),$(first hold),$(first kick),${top:--},$st" >> $RES
  tail -1 $RES
}
