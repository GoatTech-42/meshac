. "$HOME/meshac-work/meshac/rig/h/move.sh"
NAME=mc$(date +%H%M%S)
echo "scenario,hack,start,end,hp,signals,first_s,hold_s,kick_s,top,state" > $OUT
start_client || exit 1
sh "$HOME/meshac-work/meshac/rig/h/world.sh"
for c in "fall none" "fall NoFall" "water none" "water Jesus" "wall none" "wall Spider" "ladder none" "ladder FastLadder" "step none" "step Step"; do run_case $c; done
echo DONE
