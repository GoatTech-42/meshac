# 0.1.0 changelog and limits (draft, unpublished)

Not for the listing yet. Every line marked (unrun) is in code and sim-tested but has not been through a rig control run on the final jar. Drop or reword those if the rerun does not back them.

## changelog 0.1.0

first public release. meshac is a server-side fabric anticheat: players install nothing.

- movement: flight, speed, high jump, step, glide, timer, no-fall, no-clip, boat fly, anti-knockback
- combat: kill aura, reach, criticals, attack rate, attribute swapping
- building and interaction: scaffold, fast place, auto build, clicks that aren't in line of sight
- inventory and mining: fast break, nuker, impossible inventory actions
- heat model: flags add heat, heat cools off, nobody is removed for one odd packet. kick first, then growing tempbans. no permanent bans by default
- staff tools: `/mesh cases`, `/mesh case`, `/mesh status`, `/mesh watch`, `/mesh monitor on|off`, kick, ban, pardon, unban, reload
- sustained speed budget: catches a steady speed-up that stays under the per-move cap. follows soul speed and boosts (unrun)
- blink now sets the player back to where the burst started (unrun on the final jar)
- fewer false flags: powder snow climbing, delayed packets, slow falling, water, ladders, cobwebs, scaffolding, bubble columns, honey (unrun)
- bedrock players (geyser/floodgate): movement checks only, with extra room. everything else skips them
- optional anti-xray and anti-esp, off by default
- admin switches for testing: `MESHAC_TRACE`, `MESHAC_GROUND_TOL=compare` (log only, off by default)
- no-fall check watches a small window of ground claims instead of needing three odd claims in a row. a no-fall cheat that mixes its fake claims with honest packets no longer slips through (unrun)
- fake ground claims no longer reset the jump and flight chains (unrun)
- normal powder-snow struggle hops no longer risk a jump flag (unrun)
- log-only shadow measurements for a future no-fall fall-damage handback and for partial-block tolerance. off by default, no gameplay change (unrun)

versions: only claim a version here after it has been built and run. as of 10-18 the delivered 63-patch bundle compiled GREEN on all four targets (Box-reported 10-17 11:36pm, second-hand; 0067+ are tooling/docs only, and run review must re-prove a current apply manifest at the tip). No version has been RUN on the final jar.

## what it doesn't do (limits text)

- speed at or below sprint-jump pace looks like a fast legit player. meshac doesn't pretend to catch it. a steady speed over about 7.4 blocks per second does flag
- short blinks (about 0.4 s), surround, bow aimbot and boosts over 45 blocks per second are not caught
- explosion knockback is exempt on purpose
- kill aura detection is intermittent. a patient, aimed aura isn't caught every run
- flight isn't checked while a player is swimming, climbing, in a cobweb, on scaffolding, in a bubble column, near honey, or under slow falling or levitation
- bedrock players: combat, interaction, inventory and mining aren't checked, speed limits are 30% looser (a steady 8 blocks per second can pass), and gliding, vehicles, climbing, levitation, slow falling and riptide aren't checked at all. bedrock players can trigger more ground-claim flags on partial blocks like slabs and snow layers. nothing here has been tested with a real bedrock client
- meshac is tuned so normal play doesn't flag, but it has only been tested on a small rig and one live server in log-only mode. run `/mesh monitor on` first and read the cases before you let it remove anyone
- clients that only look like normal play from the server's side (a client that sprints at normal speed, for example) can't be told apart

## open before publishing

- rerun the legit controls on the final jar and replace any count quoted anywhere
- compile and run each game version that gets named
- owner decides whether the bedrock limits go in the public text
