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
setup_levitate() { X0=100 Y0=-60 Z0=0; KEYS=""; SECS=5; rc "effect give $NAME levitation 5 10 true" >/dev/null; }
setup_boat()     { X0=200 Y0=-60 Z0=0; KEYS="w"; SECS=6; rc "summon oak_boat 200.5 -60 1.5" >/dev/null; }
measure_boat()   { count oak_boat; }
setup_push()     { X0=210 Y0=-60 Z0=0; KEYS=""; SECS=6; for d in "1 0" "-1 0" "0 1" "0 -1"; do set -- $d; rc "summon cow $((210+$1)).5 -60 $((0+$2)).5 {NoAI:0b}" >/dev/null; done; }
# melee arena: four stationary zombies 3 blocks out and one straight ahead in the crosshair, three weapons in the inventory
setup_melee()    { X0=100 Y0=-60 Z0=20; KEYS=""; SECS=8; CLICK=0
  for d in "3 0" "-3 0" "0 -3" "0 3"; do set -- $d; rc "summon zombie $((100+$1)).5 -60 $((20+$2)).5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; done
  rc "give $NAME diamond_sword" >/dev/null; rc "give $NAME stone_axe" >/dev/null; rc "give $NAME mace" >/dev/null; }
measure_melee()  { count zombie; }
# --- placing, farming, inventory and survival-state scenarios ---
stone_left() { rc "fill $1 air replace stone" | grep -o "[0-9]*" | head -1; }
# build: stone in hand, looking down at the floor ahead, right-clicking once a second, walking forward (Scaffold needs movement)
setup_build()  { X0=100 Y0=-60 Z0=80; KEYS="w"; SECS=8; CLICK=3; PITCH=45; rc "give $NAME stone 64" >/dev/null; rc "fill 90 -60 82 110 -50 110 air" >/dev/null; }
measure_build() { [ "$PHASE" = after ] && stone_left "90 -61 78 110 -50 120" || echo -; }
# farm: a tilled 5x5 of wheat at full growth plus animals, with a hoe and seeds in the inventory
setup_farm()   { X0=100 Y0=-60 Z0=100; KEYS=""; SECS=8; rc "fill 98 -61 102 102 -61 106 farmland" >/dev/null; rc "fill 98 -60 102 102 -60 106 wheat[age=7]" >/dev/null; rc "give $NAME diamond_hoe" >/dev/null; rc "give $NAME wheat_seeds 16" >/dev/null; rc "give $NAME bone_meal 16" >/dev/null; rc "summon cow 104.5 -60 100.5 {NoAI:1b}" >/dev/null; }
measure_farm() { rc "execute if block 100 -60 104 wheat[age=7]" | grep -o "passed\|failed"; }
# tree: oak logs and leaves in a small tree, axe in hand
setup_tree()   { X0=100 Y0=-60 Z0=120; KEYS=""; SECS=10; rc "fill 104 -60 124 104 -56 124 oak_log" >/dev/null; rc "fill 102 -56 122 106 -54 126 oak_leaves" >/dev/null; rc "give $NAME diamond_axe" >/dev/null; }
measure_tree() { rc "execute if block 104 -58 124 oak_log" | grep -o "passed\|failed"; }
# sign: an oak sign to edit. AutoSign writes on place/edit; measured by whether any sign text is set.
setup_sign()   { X0=100 Y0=-60 Z0=140; KEYS=""; SECS=6; CLICK=3; PITCH=30; rc "give $NAME oak_sign 4" >/dev/null; }
measure_sign() { echo -; }
# chest: a chest three blocks ahead holding diamonds, player looking at it and right-clicking it
setup_chest()  { X0=100 Y0=-60 Z0=160; KEYS=""; SECS=8; CLICK=3; PITCH=15; rc "setblock 100 -60 163 chest{Items:[{Slot:0,id:\"minecraft:diamond\",count:32},{Slot:1,id:\"minecraft:gold_ingot\",count:20}]}" >/dev/null; }
measure_chest() { rc "data get block 100 -60 163 Items" | grep -o "id: \"minecraft:[a-z_]*\"" | wc -l; }
# junk: a hotbar full of dirt and cobblestone
setup_junk()   { X0=100 Y0=-60 Z0=180; KEYS=""; SECS=8; rc "give $NAME dirt 64" >/dev/null; rc "give $NAME cobblestone 64" >/dev/null; rc "give $NAME bread 8" >/dev/null; }
measure_junk() { count item; }
# food: hungry player holding bread
setup_food()   { X0=100 Y0=-60 Z0=200; KEYS=""; SECS=10; rc "give $NAME bread 16" >/dev/null; rc "effect give $NAME hunger 8 60 true" >/dev/null; }
measure_food() { rc "data get entity $NAME foodLevel" | grep -o "[0-9]*$"; }
# lowhp: 5 hearts left, armor, a totem, healing potions and soup in the inventory
setup_lowhp()  { X0=100 Y0=-60 Z0=220; KEYS=""; SECS=8; rc "give $NAME diamond_chestplate" >/dev/null; rc "give $NAME totem_of_undying" >/dev/null; rc "give $NAME splash_potion[potion_contents={potion:\"minecraft:strong_healing\"}] 3" >/dev/null; rc "give $NAME mushroom_stew 3" >/dev/null; rc "damage $NAME 15 generic" >/dev/null; }
measure_lowhp() { echo "$(hp)hp"; }
# death: the player dies and the hack has to press respawn
setup_death()  { X0=100 Y0=-60 Z0=240; KEYS=""; SECS=8; }
post_death()   { rc "kill $NAME" >/dev/null; }
measure_death() { hp; }
# creative: item generators and potion makers only work in creative
setup_creative() { X0=100 Y0=-60 Z0=260; KEYS=""; SECS=8; }
post_creative()  { rc "gamemode creative $NAME" >/dev/null; }
measure_creative() { count item; }
# ranged: a bow, arrows, and two zombies 12 blocks away
setup_ranged() { X0=100 Y0=-60 Z0=280; KEYS=""; SECS=10; CLICK=3; rc "give $NAME bow" >/dev/null; rc "give $NAME arrow 32" >/dev/null; rc "summon zombie 100.5 -60 292.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; rc "summon zombie 104.5 -60 292.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; }
measure_ranged() { rc "data get entity @e[type=zombie,limit=1,sort=nearest] Health" | grep -o "[0-9.]*f" | tr -d "f\n"; }
# throw: snowballs
setup_throw()  { X0=100 Y0=-60 Z0=300; KEYS=""; SECS=6; CLICK=3; rc "give $NAME snowball 16" >/dev/null; }
measure_throw() { count snowball; }
post_boat() { rc "ride $NAME mount @e[type=oak_boat,limit=1,sort=nearest]" >/dev/null; sleep 1; }
# legit control for the melee arena: a person clicking once a second at the zombie straight ahead
setup_meleeclick() { setup_melee; CLICK=1; JITTER=1; }
measure_meleeclick() { count zombie; }
# reach: one zombie 5.5 blocks ahead, clicking. Vanilla cannot hit it from here.
setup_reach() { X0=100 Y0=-60 Z0=40; KEYS=""; SECS=8; CLICK=1; rc "summon zombie 100.5 -60 45.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; rc "give $NAME diamond_sword" >/dev/null; }
measure_reach() { rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f" | tr -d "f\n"; }
# mine: a block of stone around the player, with a pickaxe. After the run, count what is left.
setup_mine() { X0=100 Y0=-60 Z0=60; KEYS=""; SECS=10; rc "fill 96 -61 56 104 -56 64 stone" >/dev/null; rc "fill 100 -60 60 100 -59 60 air" >/dev/null; rc "give $NAME diamond_pickaxe" >/dev/null; }
measure_mine() { if [ "$PHASE" = after ]; then rc "fill 96 -60 56 104 -56 64 air replace stone" | grep -o "[0-9]*" | head -1; else echo -; fi; }
# melee against one very tanky zombie in the crosshair: the clicker keeps landing hits for the whole run (Criticals, AntiKnockback, TriggerBot, AutoSword need hits to happen)
setup_meleetank()   { X0=100 Y0=-60 Z0=20; KEYS=""; SECS=12; CLICK=1; JITTER=1
  rc "summon zombie 100.5 -60 23.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null
  rc "give $NAME diamond_sword" >/dev/null; rc "give $NAME stone_axe" >/dev/null; rc "give $NAME mace" >/dev/null; }
measure_meleetank() { rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f\?$" | head -1; }
# one tanky zombie 5 blocks out: only a reach hack can hit it (vanilla reach is 3)
setup_meleefar()   { X0=100 Y0=-60 Z0=20; KEYS=""; SECS=12; CLICK=1; JITTER=1
  rc "summon zombie 100.5 -60 25.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null; }
measure_meleefar() { rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f\?$" | head -1; }
# like meleetank but the mace is the only item, so it is in the hand (MaceDMG only acts with a mace held)
setup_meleemace()   { X0=100 Y0=-60 Z0=20; KEYS=""; SECS=12; CLICK=1; JITTER=1
  rc "summon zombie 100.5 -60 23.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null
  rc "give $NAME mace" >/dev/null; }
measure_meleemace() { rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f\?$" | head -1; }
# double-tap jump to start creative-style flight, then hold jump to climb (CreativeFlight only sets mayfly; the player has to toggle flying)
setup_flytoggle() { X0=100 Y0=-60 Z0=400; KEYS="space"; SECS=6; PRE='X keydown space; sleep 0.07; X keyup space; sleep 0.1; X keydown space; sleep 0.07; X keyup space; sleep 0.2'; }
# sit in a boat and press forward + jump (BoatFly)
setup_boatride()  { X0=200 Y0=-60 Z0=420; KEYS="w space"; SECS=6; rc "summon oak_boat 200.5 -60 420.5" >/dev/null; PRE='rc "ride $NAME mount @e[type=oak_boat,limit=1,sort=nearest]" >/dev/null; sleep 1'; }
# the player starts buried inside a stone block; NoClip only acts when the player is inside a solid block
setup_buried() { X0=160 Y0=-60 Z0=30; KEYS="w"; SECS=6; rc "fill 159 -61 30 161 -58 32 stone" >/dev/null; rc "effect give $NAME resistance 60 255 true" >/dev/null; }
setup_bhop() { X0=100 Y0=-60 Z0=500; KEYS="w"; SECS=8; }
# weak item in slot 0, sword in slot 1, tanky zombie: AutoSword swaps to the sword when you hit
setup_swordswap() { X0=100 Y0=-60 Z0=600; KEYS=""; SECS=10; CLICK=1; JITTER=1
  rc "summon zombie 100.5 -60 603.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null
  rc "give $NAME stick" >/dev/null; rc "give $NAME diamond_sword" >/dev/null; }
measure_swordswap() { rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f\?$" | head -1; }
setup_attrswap() { setup_swordswap; Z0=600; CLICK=0; PRE='say ".t Killaura"'; }
measure_attrswap() { measure_swordswap; }
setup_swapback() { setup_swordswap; PRE='X key 2; sleep 0.5'; }
measure_swapback() { measure_swordswap; }
