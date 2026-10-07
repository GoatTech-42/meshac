# Veil client test spec (rig, isolated from live world; protocol bots plus Luke's real clients Wurst, Meteor)
World: flat-stone arena, 20x20x20 buried ore block field at y=-30..-10 (diamond, iron, ancient debris equivalents as per dimension), cave pocket with 3 exposed ores, 3 chests buried, 3 chests in a lit room.
## Xray (hack: Meteor Xray, Wurst X-Ray, both with ore lists on)
X1 Stand inside the cave pocket, enable xray. Pass: only the 3 exposed ores render. The buried 20-block field shows real ores only where an exposed neighbour exists. Count rendered ores via screenshot or the client's block state query (protocol bot: read chunk, count ore states).
X2 Seed pattern: the same buried field with fakeOreRatio 0.15, compare the ore count a bot sees to the real count. Pass: real positions are not distinguishable (bot sees about 15% extra fakes, no real buried one shows). Also run with ratio 0: buried ores are plain stone.
X3 Determinism: reconnect and relog 5 times. Pass: the identical fake positions every time.
X4 Reveal: dig into a buried ore by one block (legit mining). Pass: the ore is visible within 1 tick after the wall block breaks. Dig toward and past the ore, pistons and TNT: same.
X5 Legit control: 10 minutes of scripted legit mining (branch mine) on veil vs veil-off; recorded visible ores on the exposed faces must be identical.
X6 Chest ESP: Meteor ChestESP/StorageESP on buried chests: not rendered. Lit room chests: rendered.
X7 Performance: 20 bots join in a fresh area, ms per chunk send and tick time p99, memory delta. Pass: p99 tick delta under 1 ms, chunk send under 1 ms avg.
## ESP / freecam (hack: Meteor ESP, Tracers, Nametags, Freecam; Wurst PlayerESP, Tracers)
E1 Victim bot behind a stone wall at 20 blocks, ESP on. Pass: the viewer's client entity list does not contain the victim after 250 ms. No tracer line.
E2 Victim stationary in the open at 20 blocks: always present.
E3 Corner peek (controls, protocol bot as viewer, 100 runs, victim walking at 5.6 b/s around a corner, then 30 runs at 100/200/300 ms ping): the victim must be present in the viewer entity list before or at the first tick the victim has line of sight (computed from the rig). Any run where it appears later is a failure.
E4 Near: victim behind a wall at 6 blocks: always present (inside nearRadius).
E5 Glass, leaves, fence between viewer and victim: victim present.
E6 Combat: the viewer hits a victim and then victim goes behind a wall: present 3 s after the last hit.
E7 Vehicle: boat or horse passengers present. Spectator/creative viewer: sees everything.
E8 Freecam: Meteor Freecam moved out through a wall with the real player at the original spot, looking at a victim behind a wall: the victim is not rendered. Looking at a victim in the open: rendered.
E9 Mobs: zombie behind a wall at 20 blocks, mob ESP on: absent. Zombie in the open: present.
E10 Dimension change, respawn, teleport: all entities in view appear within normal vanilla timing (first 40 ticks no culling).
E11 Perf: 40 bots, 500 entities, 200 ray cap: tick ms p99 delta under 1.5 ms, check that the queue drains every 4 ticks.
## Pass bar
Zero legit-visible differences (X4, X5, E2-E7, E10, all E3 runs). Every hack scenario X1, X2, X6, E1, E8, E9 hides what it should.
