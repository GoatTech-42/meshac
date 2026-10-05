# Scenario setups. Standing height on the flat world is y=-60. Lanes for the movement course are built by world.sh.
count() { rc "execute if entity @e[type=$1]" | grep -o "ount: [0-9]*" | grep -o "[0-9]*" || echo 0; }
setup_walk()     { X0=100 Y0=-60 Z0=0; KEYS="w space"; SECS=8; }
setup_fall()     { X0=110 Y0=-52 Z0=0; KEYS="w"; SECS=5; }
setup_water()    { X0=120 Y0=-60 Z0=0; KEYS="w"; SECS=8; }
setup_wall()     { X0=130 Y0=-60 Z0=0; KEYS="w"; SECS=6; }
setup_ladder()   { X0=140 Y0=-60 Z0=0; KEYS="w"; SECS=6; }
setup_step()     { X0=150 Y0=-60 Z0=0; KEYS="w"; SECS=6; }
setup_web()      { X0=170 Y0=-60 Z0=0; KEYS="w"; SECS=6; }
setup_soulsand() { X0=180 Y0=-60 Z0=0; KEYS="w"; SECS=6; }
setup_snow()     { X0=190 Y0=-60 Z0=0; KEYS="w"; SECS=6; }
setup_levitate() { X0=100 Y0=-60 Z0=0; KEYS=""; SECS=5; rc "effect give $NAME levitation 2 1 true" >/dev/null; }
setup_boat()     { X0=200 Y0=-60 Z0=0; KEYS="w"; SECS=6; rc "summon oak_boat 200.5 -60 1.5" >/dev/null; }
measure_boat()   { count oak_boat; }
setup_push()     { X0=210 Y0=-60 Z0=0; KEYS=""; SECS=6; for d in "1 0" "-1 0" "0 1" "0 -1"; do set -- $d; rc "summon cow $((210+$1)).5 -60 $((0+$2)).5 {NoAI:0b}" >/dev/null; done; }
# melee arena: four stationary zombies 3 blocks out and one straight ahead in the crosshair, three weapons in the inventory
setup_melee()    { X0=100 Y0=-60 Z0=20; KEYS=""; SECS=8; CLICK=0
  for d in "3 0" "-3 0" "0 -3" "0 3"; do set -- $d; rc "summon zombie $((100+$1)).5 -60 $((20+$2)).5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; done
  rc "give $NAME diamond_sword" >/dev/null; rc "give $NAME stone_axe" >/dev/null; rc "give $NAME mace" >/dev/null; }
measure_melee()  { count zombie; }
post_boat() { rc "ride $NAME mount @e[type=oak_boat,limit=1,sort=nearest]" >/dev/null; sleep 1; }
# legit control for the melee arena: a person clicking once a second at the zombie straight ahead
setup_meleeclick() { setup_melee; CLICK=1; rc "tp $NAME 100.5 -60 20.5 0 0" >/dev/null; YAW=0; }
measure_meleeclick() { count zombie; }
# reach: one zombie 5.5 blocks ahead, clicking. Vanilla cannot hit it from here.
setup_reach() { X0=100 Y0=-60 Z0=40; KEYS=""; SECS=8; CLICK=1; rc "summon zombie 100.5 -60 45.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; rc "give $NAME diamond_sword" >/dev/null; }
measure_reach() { rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f" | tr -d "f\n"; }
# mine: a hollow ring of stone around the player, with a pickaxe. After the run, count what is left.
setup_mine() { X0=100 Y0=-60 Z0=60; KEYS=""; SECS=10; rc "fill 96 -61 56 104 -56 64 stone" >/dev/null; rc "fill 100 -60 60 100 -59 60 air" >/dev/null; rc "give $NAME diamond_pickaxe" >/dev/null; }
measure_mine() { [ "$PHASE" = after ] && rc "fill 96 -60 56 104 -56 64 air replace stone" | grep -o "[0-9]*" | head -1 || echo -; }
