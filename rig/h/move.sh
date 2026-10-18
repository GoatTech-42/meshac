# usage: move.sh <scenario> <hack|none> ...   Runs one hack (or a legit control) through a movement scenario and appends a CSV row.
. "$(dirname "$0")/lib.sh"
OUT=${OUT:-$HOME/meshac-work/results-move.csv}
case_params() { # sets X0 Y0 Z0 KEYS SECS
  case $1 in
    walk)     X0=100 Y0=-60 Z0=0 KEYS="w space" SECS=8;;
    fall)     X0=110 Y0=-52 Z0=0 KEYS="w" SECS=5;;
    water)    X0=120 Y0=-60 Z0=0 KEYS="w" SECS=8;;
    wall)     X0=130 Y0=-60 Z0=0 KEYS="w" SECS=6;;
    ladder)   X0=140 Y0=-60 Z0=0 KEYS="w" SECS=6;;
    step)     X0=150 Y0=-60 Z0=0 KEYS="w" SECS=6;;
    web)      X0=170 Y0=-60 Z0=0 KEYS="w" SECS=6;;
    soulsand) X0=180 Y0=-60 Z0=0 KEYS="w" SECS=6;;
    snow)     X0=190 Y0=-60 Z0=0 KEYS="w" SECS=6;;
    levitate) X0=100 Y0=-60 Z0=0 KEYS="" SECS=5;;
  esac
}
run_case() { # scenario hack
  S=$1; H=$2; case_params $S
  ensure || { echo "$S,$H,-,-,-,-,-,-,-,client lost" >> $OUT; return; }
  rc "difficulty easy" >/dev/null; rc "gamerule spawn_mobs false" >/dev/null; rc "gamerule natural_health_regeneration false" >/dev/null; rc "kill @e[type=!player,type=!boat]" >/dev/null; rc "gamemode survival $NAME" >/dev/null
  rc "effect clear $NAME" >/dev/null; rc "effect give $NAME instant_health 1 10 true" >/dev/null
  rc "tp $NAME $X0.5 $Y0 $Z0.5 0 0" >/dev/null; sleep 2
  [ $S = levitate ] && rc "effect give $NAME levitation 5 10 true" >/dev/null
  p0=$(pos); h0=$(hp)
  [ "$H" != none ] && say ".t $H"
  iso=$(date -u +%Y-%m-%dT%H:%M:%S); t0=$(date +%s.%N)
  for k in $KEYS; do X keydown $k; done; sleep $((SECS/2))
  docker exec -e DISPLAY=:99 $CL import -window root /tmp/s.png 2>/dev/null; mkdir -p ~/meshac-work/shots; docker cp $CL:/tmp/s.png ~/meshac-work/shots/$S-$H.png 2>/dev/null
  sleep $((SECS-SECS/2)); for k in $KEYS; do X keyup $k; done; sleep 2
  st=connected; connected || st=kicked
  p1=$(pos); h1=$(hp)
  [ $st = connected ] && [ "$H" != none ] && say ".t $H"
  L=$(docker logs -t --since $iso $RIG 2>&1 | grep "SIGNAL $NAME")
  n=$(echo "$L" | grep -c SIGNAL)
  first() { ts=$(echo "$L" | grep -m1 "$1" | cut -d" " -f1); [ -z "$ts" ] && { echo -; return; }; awk -v a="$(date -d "$ts" +%s.%N)" -v b="$t0" 'BEGIN{printf "%.1f", a-b}'; }
  top=$(echo "$L" | sed 's/.*SIGNAL [^ ]* //' | awk '{print $1}' | sort | uniq -c | sort -rn | head -1 | awk '{print $2}')
  echo "$S,$H,\"$p0\",\"$p1\",$h0>$h1,$n,$(first SIGNAL),$(first hold),$(first kick),${top:--},$st" >> $OUT
  echo "$(tail -1 $OUT)"
  [ $st = kicked ] && sleep 1
}
