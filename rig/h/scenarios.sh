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
setup_chest()  { X0=100 Y0=-60 Z0=160; KEYS=""; SECS=8; CLICK=3; PITCH=15; rc "forceload add 100 163" >/dev/null; sleep 2; rc "setblock 100 -60 163 air" >/dev/null; rc "setblock 100 -60 163 chest{Items:[{Slot:0,id:\"minecraft:diamond\",count:8},{Slot:1,id:\"minecraft:gold_ingot\",count:8},{Slot:2,id:\"minecraft:iron_ingot\",count:8},{Slot:3,id:\"minecraft:emerald\",count:8},{Slot:4,id:\"minecraft:coal\",count:8},{Slot:5,id:\"minecraft:copper_ingot\",count:8},{Slot:6,id:\"minecraft:lapis_lazuli\",count:8},{Slot:7,id:\"minecraft:redstone\",count:8},{Slot:8,id:\"minecraft:quartz\",count:8},{Slot:9,id:\"minecraft:amethyst_shard\",count:8},{Slot:10,id:\"minecraft:stick\",count:8},{Slot:11,id:\"minecraft:bone\",count:8}]}" >/dev/null; }
measure_chest() { rc "data get block 100 -60 163 Items" | grep -o "id: \"minecraft:[a-z_]*\"" | wc -l; }
# junk: a hotbar full of dirt and cobblestone
setup_junk()   { X0=100 Y0=-60 Z0=180; KEYS=""; SECS=8; rc "give $NAME dirt 64" >/dev/null; rc "give $NAME cobblestone 64" >/dev/null; rc "give $NAME bread 8" >/dev/null; rc "give $NAME wheat_seeds 32" >/dev/null; rc "give $NAME rotten_flesh 32" >/dev/null; }
measure_junk() { count item; }
# food: hungry player holding bread
setup_food()   { X0=100 Y0=-60 Z0=200; KEYS=""; SECS=10; rc "give $NAME bread 16" >/dev/null; rc "effect give $NAME hunger 8 60 true" >/dev/null; }
measure_food() { rc "data get entity $NAME foodLevel" | grep -o "[0-9]*$"; }
# lowhp: 5 hearts left, armor, a totem, healing potions and soup in the inventory
setup_lowhp()  { X0=100 Y0=-60 Z0=220; KEYS=""; SECS=8; rc "give $NAME diamond_chestplate" >/dev/null; rc "give $NAME diamond_helmet" >/dev/null; rc "give $NAME diamond_leggings" >/dev/null; rc "give $NAME diamond_boots" >/dev/null; rc "give $NAME totem_of_undying" >/dev/null; rc "give $NAME splash_potion[potion_contents={potion:\"minecraft:strong_healing\"}] 3" >/dev/null; rc "give $NAME mushroom_stew 3" >/dev/null; rc "damage $NAME 15 generic" >/dev/null; }
measure_lowhp() { echo "$(hp)hp"; }
# death: the player dies and the hack has to press respawn
setup_death()  { X0=100 Y0=-60 Z0=240; KEYS=""; SECS=8; }
post_death()   { rc "kill $NAME" >/dev/null; }
measure_death() { hp; }
# creative: item generators and potion makers only work in creative
setup_creative() { X0=100 Y0=-60 Z0=260; KEYS=""; SECS=8; rc "clear $NAME" >/dev/null; }
post_creative()  { rc "gamemode creative $NAME" >/dev/null; }
# ranged: a bow, arrows, and two zombies 12 blocks away
setup_ranged() { X0=100 Y0=-60 Z0=280; KEYS=""; SECS=10; CLICK=3; rc "give $NAME bow" >/dev/null; rc "give $NAME arrow 32" >/dev/null; rc "summon zombie 100.5 -60 292.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; rc "summon zombie 104.5 -60 292.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; }
measure_ranged() { rc "data get entity @e[type=zombie,limit=1,sort=nearest] Health" | grep -o "[0-9.]*f" | tr -d "f\n"; }
# throw: snowballs
setup_throw()  { X0=100 Y0=-60 Z0=300; KEYS=""; SECS=6; PITCH=-10; rc "give $NAME snowball 16" >/dev/null; PRE="X mousedown 3; (sleep 6; X mouseup 3) &"; }
measure_throw() { rc "clear $NAME snowball 0" | grep -o "[0-9]\+ item" | head -1; }
post_boat() { rc "ride $NAME mount @e[type=oak_boat,limit=1,sort=nearest]" >/dev/null; sleep 1; }
# legit control for the melee arena: a person clicking once a second at the zombie straight ahead
setup_meleeclick() { setup_melee; CLICK=1; JITTER=1; }
measure_meleeclick() { count zombie; }
# reach: one zombie 5.5 blocks ahead, clicking. Vanilla cannot hit it from here.
setup_reach() { X0=100 Y0=-60 Z0=40; KEYS=""; SECS=8; CLICK=1; rc "summon zombie 100.5 -60 45.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}" >/dev/null; rc "give $NAME diamond_sword" >/dev/null; }
measure_reach() { rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f" | tr -d "f\n"; }
# mine: a block of stone around the player, with a pickaxe. After the run, count what is left.
setup_mine() { rm -f /tmp/mine.flag; X0=100 Y0=-60 Z0=58; KEYS=""; SECS=10; PITCH=0; YAW=180; rc "fill 96 -61 50 104 -55 57 stone" >/dev/null; rc "fill 96 -60 58 104 -55 62 air" >/dev/null; rc "give $NAME diamond_pickaxe" >/dev/null; PRE='X mousedown 1; (sleep 10; X mouseup 1) &'; }
measure_mine() { if [ -f /tmp/mine.flag ]; then rm -f /tmp/mine.flag; rc "fill 96 -61 50 104 -55 57 air replace stone" | grep -o "[0-9]\+" | head -1; else touch /tmp/mine.flag; echo 504; fi; } # stones left: 504 at start, the end value is counted then cleared
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
# an awake zombie keeps hitting a standing player; AntiKnockback cancels the push
setup_zombiehit() { X0=100 Y0=-60 Z0=700; KEYS=""; SECS=20; rc "effect give $NAME regeneration 60 4 true" >/dev/null; rc "effect give $NAME resistance 60 1 true" >/dev/null
  PRE='for o in "100.5 702.5" "98.5 700.5" "102.5 700.5"; do rc "summon zombie ${o% *} -60 ${o#* } {PersistenceRequired:1b}" >/dev/null; done'; }
# draw and release a bow at standing zombies 12 blocks away (BowAimbot aims the shot for you)
setup_bowshot() { setup_ranged; Z0=280; CLICK=0; SECS=2; PRE='for i in 1 2 3 4 5 6; do X mousedown 3; sleep 1.3; X mouseup 3; sleep 0.6; done'; }
measure_bowshot() { rc "data get entity @e[type=zombie,limit=1,sort=nearest] Health" | grep -o "[0-9.]*f\?$" | head -1; }
# a small stone platform 20 blocks up with only air beyond it: ScaffoldWalk lays blocks under the feet, AirPlace places into the air at the crosshair
setup_scaffold() { X0=100 Y0=-41 Z0=800; KEYS="w"; SECS=8; PITCH=20; CLICK=3; rc "forceload add 90 790 110 860" >/dev/null; sleep 2; rc "fill 90 -45 790 110 -38 860 air" >/dev/null; rc "fill 98 -42 799 102 -42 801 stone" >/dev/null; rc "give $NAME stone 64" >/dev/null; rc "effect give $NAME resistance 60 255 true" >/dev/null; }
setup_fastplace() { X0=100 Y0=-60 Z0=84; KEYS=""; SECS=8; PITCH=45; rc "give $NAME stone 64" >/dev/null; rc "fill 96 -60 80 104 -55 92 air" >/dev/null; PRE='X mousedown 3; (n=0; while [ $n -lt 30 ]; do sleep 0.25; rc "fill 98 -60 85 102 -59 89 air replace stone" >/dev/null; n=$((n+1)); done; X mouseup 3) &'; }
measure_fastplace() { rc "give $NAME stone 0" | grep -o "[0-9]\+" | head -1; }
setup_bonemeal() { X0=100 Y0=-60 Z0=330; KEYS=""; SECS=8; rc "fill 98 -61 332 102 -61 336 farmland" >/dev/null; rc "fill 98 -60 332 102 -60 336 wheat[age=0]" >/dev/null; rc "give $NAME bone_meal 64" >/dev/null; }
measure_bonemeal() { rc "execute if block 100 -60 334 wheat[age=7]" | grep -o "passed\|failed"; }
setup_feed() { X0=100 Y0=-60 Z0=350; KEYS=""; SECS=8; rc "forceload add 100 354" >/dev/null; sleep 2; rc "kill @e[type=cow]" >/dev/null; for i in 1 2 3 4; do rc "summon cow $((98+i)) -60 354" >/dev/null; done; rc "give $NAME wheat 32" >/dev/null; }
measure_feed() { rc "execute as @e[type=cow] if data entity @s {InLove:0}" | grep -o "[0-9]*" | tail -1; rc "execute if entity @e[type=cow,nbt={InLove:0}]" | grep -o "passed\|failed"; }
measure_creative() { rc "clear $NAME * 0" | sed -n "s/^Found \([0-9]*\) .*/\1/p;s/^No items.*/0/p" | head -1; }
# crystal: three end crystals on obsidian around the player, crystals in hand. CrystalAura breaks them.
setup_crystal()  { X0=100 Y0=-60 Z0=400; KEYS=""; SECS=8; rc "forceload add 100 400" >/dev/null; sleep 2; rc "kill @e[type=end_crystal]" >/dev/null; for d in "3 0" "-3 0" "0 3"; do set -- $d; rc "setblock $((100+$1)) -61 $((400+$2)) obsidian" >/dev/null; rc "summon end_crystal $((100+$1)).5 -60 $((400+$2)).5 {ShowBottom:0b}" >/dev/null; done; rc "give $NAME end_crystal 8" >/dev/null; }
measure_crystal() { count end_crystal; }
# totem: a totem in the off hand and three more in the inventory, killing blows every 3 s. A hack refills the off hand; the control does nothing.
setup_totem() { X0=100 Y0=-60 Z0=420; KEYS=""; SECS=10; rc "forceload add 100 420" >/dev/null; rc "item replace entity $NAME weapon.offhand with totem_of_undying" >/dev/null; rc "item replace entity $NAME hotbar.1 with totem_of_undying 3" >/dev/null; rc "give $NAME stone 1" >/dev/null
  ( for i in 1 2 3; do sleep 3; rc "damage $NAME 40 generic" >/dev/null; done ) & }
measure_totem() { rc "data get entity $NAME Inventory" | grep -o "totem_of_undying" | wc -l; }
# totemf: the human version. After each pop a person reaches for the F key about 0.4 s later and swaps in a totem.
setup_totemf() { setup_totem; SECS=11; rc "item replace entity $NAME hotbar.0 with totem_of_undying" >/dev/null
  ( for i in 1 2 3; do sleep 3.4; X key f; sleep 0.1; done ) & }
measure_totemf() { measure_totem; }
# totemz: the fastest human. The F key about 0.12 s after each pop, which is at the edge of what a person can see and react to.
setup_totemz() { setup_totem; SECS=11; rc "item replace entity $NAME hotbar.0 with totem_of_undying" >/dev/null
  ( for i in 1 2 3; do sleep 3.12; X key f; sleep 0.1; done ) & }
measure_totemz() { measure_totem; }
# totem1: one totem in the off hand, one spare, one killing blow. The hack refills at once and the first fill must already be refused.
setup_totem1() { X0=100 Y0=-60 Z0=420; KEYS=""; SECS=8; rc "forceload add 100 420" >/dev/null; rc "item replace entity $NAME weapon.offhand with totem_of_undying" >/dev/null; rc "item replace entity $NAME hotbar.1 with totem_of_undying 1" >/dev/null
  PRE='rc "damage $NAME 40 generic" >/dev/null'; }
measure_totem1() { rc "data get entity $NAME equipment.offhand" | grep -o "totem_of_undying" | wc -l; }
