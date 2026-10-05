. $HOME/meshac-work/meshac/rig/h/lib.sh
NAME=mc$(date +%H%M%S)
start_client && echo connected $NAME
rc "gamemode survival $NAME"; rc "tp $NAME 100 -60 0 0 0"; sleep 2; pos
say ".t Flight"; X keydown w; X keydown space; sleep 8; X keyup space; X keyup w
pos; connected && echo still-in || echo kicked
ensure && echo reconnected; pos
