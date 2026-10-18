# usage: batch.sh <scenario...>   For each scenario: one legit control, then every module assigned to it in modules.tsv.
D="$(dirname "$0")"; . "$D/run.sh"
NAME=mc$(date +%H%M%S)
[ -s $RES ] || echo "scenario,hack,start,end,hp,measure,signals,first_s,hold_s,kick_s,top,state" > $RES
start_client || exit 1
sh "$D/world.sh" >/dev/null
for s in "$@"; do
  run_case $s none
  for h in $(awk -F'|' -v s=$s '$3==s{print $1}' "$D/modules.tsv"); do run_case $s $h; done
done
echo DONE
