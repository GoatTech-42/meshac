#!/bin/sh
# usage: run-all.sh legit speed fly ...   Each mode: fresh player (spawns on the ground), run bot, count meshac FLAG lines.
cd "$(dirname "$0")"
for m in "$@"; do
  t0=$(date -u +%Y-%m-%dT%H:%M:%S)
  docker run --rm --network meshac-net --memory 512m --cpus 1 -v "$PWD":/w -w /w -e MODE=$m -e MCVER=${MCVER:-26.1} node:22-slim timeout 90 node cheat.mjs 2>&1 | grep -E "start|end|kicked" | tr "\n" " "
  echo; docker logs --since $t0 meshac-rig 2>&1 | grep "FLAG b_$m" | sed "s/.*FLAG b_[a-z]*//" | cut -c5-30 | sort | uniq -c | head -3; echo "RESULT $m flags=$(docker logs --since $t0 meshac-rig 2>&1 | grep -c "FLAG b_$m")"
done
