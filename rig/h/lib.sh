# Shared helpers for the meshac scenario harness. Source this file.
RIG=${RIG:-meshac-rig}; CL=${CL:-meshac-wurst}
IP() { docker inspect $RIG --format '{{(index .NetworkSettings.Networks "meshac-net").IPAddress}}'; }
rc() { python3 /tmp/rcon.py "$(IP)" 25575 meshacrig "$1"; }
X() { docker exec -e DISPLAY=:99 $CL xdotool "$@"; }
say() { X key --delay 80 t; sleep 0.7; X type --delay 35 "$1"; X key Return; sleep 0.7; }
joined() { docker logs $RIG 2>&1 | grep -c "$NAME joined the game"; }
left()   { docker logs $RIG 2>&1 | grep -c "$NAME left the game"; }
connected() { [ "$(joined)" -gt "$(left)" ]; }
reset_hacks() { echo "[]" > ~/meshac-work/clients/wurst/run/wurst/enabled-hacks.json; }
start_client() {
  reset_hacks; docker rm -f $CL >/dev/null 2>&1
  (cd ~/meshac-work/meshac/rig/client && ./run-client.sh ~/meshac-work/clients/wurst ~/meshac-work/clients/gh $NAME >/dev/null)
  for i in $(seq 1 45); do sleep 4; connected && { sleep 8; return 0; }; done; return 1
}
ensure() { if connected && [ "$(hp)" != 0.0 ]; then return 0; fi; connected && rc "kick $NAME" >/dev/null; NAME=mc$(date +%H%M%S); start_client; }
pos() { rc "data get entity $NAME Pos" | grep -o '\[.*\]' | tr -d '[]dD \n'; }
hp()  { rc "data get entity $NAME Health" | grep -o '[0-9.]*f' | tr -d 'f\n'; }
