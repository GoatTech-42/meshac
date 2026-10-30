#!/bin/sh
# Signal-presence checks for injected protocol streams, not enforcement/physics proof.
# Requires externally prepared rig; this script does not establish runtime authority,
# jar/config/world identity or resource admission. Do not run without that admission.
cd "$(dirname "$0")" || exit 1
if [ "$#" -eq 0 ]; then
  set -- hover ascend airjump speed timer step walk arc sprintarc slowcadence
fi
# Reject every unknown mode before starting any client.
for m do
  case $m in hover|ascend|airjump|speed|timer|step|walk|arc|sprintarc|slowcadence) ;; *) echo "FAIL unknown mode $m"; exit 2;; esac
done
fail=0
for m do
  case $m in
    hover) want="fly";; ascend) want="high|fly";; airjump) want="air";;
    speed) want="speed";; timer) want="timer";; step) want="step";;
    walk|arc|sprintarc|slowcadence) want="NONE";;
  esac
  t0=$(date -u +%Y-%m-%dT%H:%M:%S) || exit 1
  if out=$(docker run --rm --network meshac-net --memory 512m --cpus 1 -v "$PWD":/w -w /w -e MODE="$m" -e MCVER="${MCVER:-26.1}" node:22-slim timeout 120 node positive.mjs 2>&1); then
    :
  else
    rc=$?; printf '%s\n' "$out"; echo "FAIL $m: client/container exit $rc"; fail=1; continue
  fi
  printf '%s\n' "$out"
  player=$(printf '%s\n' "$out" | sed -n 's/^PLAYER //p')
  case $player in bp_????????????) ;; *) echo "FAIL $m: missing/invalid player identity"; fail=1; continue;; esac
  if [ "$(printf '%s\n' "$out" | grep -Fxc "CONNECTED $player $m")" != 1 ] ||
     [ "$(printf '%s\n' "$out" | grep -Fxc "COMPLETE $player $m")" != 1 ]; then
    echo "FAIL $m: connection/completion receipt missing or duplicated"; fail=1; continue
  fi
  if logs=$(docker logs --since "$t0" meshac-rig 2>&1); then :; else
    rc=$?; printf '%s\n' "$logs"; echo "FAIL $m: server log read exit $rc"; fail=1; continue
  fi
  if sigs=$(printf '%s\n' "$logs" | awk -v player="$player" '{ for(i=1;i<NF;i++) if($i=="SIGNAL" && $(i+1)==player) print $(i+2) }'); then :; else
    echo "FAIL $m: signal parser failed"; fail=1; continue
  fi
  if [ "$want" = NONE ]; then
    if [ -z "$sigs" ]; then echo "OBSERVED $m: no signals in returned log"; else echo "FAIL $m: control stream signalled: $sigs"; fail=1; fi
  else
    if printf '%s\n' "$sigs" | tr ' ' '\n' | grep -qE "^($want)$"; then echo "OBSERVED $m: got [$sigs] want $want"; else echo "FAIL $m: got [$sigs] want $want"; fail=1; fi
  fi
done
exit "$fail"
