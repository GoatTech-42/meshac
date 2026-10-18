# Modrinth listing draft v3 (not published)

v3, 10-17: reviewer round B on v2 (CHANGES): every combat log-only claim now carries the
monitor-mode-on condition, and the monitor-mode DEFAULT is stated from the repo (it is OFF,
so combat enforces out of box - see the monitor line under "How it punishes"). v1 and v2
kept untouched. Verification notes are at the bottom; nothing here is published.

**Name:** meshac
**Slug:** meshac
**Summary (under 256 chars):** A native Fabric anticheat. Runs on the server, players install nothing. Watches movement, combat (log-only in monitor mode), building, inventory, mining. Built to limit false flags on normal play, with a log-only monitor mode so you can check it first.

**Project type:** Mod
**Loader:** Fabric
**Environment:** Server-side required, client-side not needed
**Categories:** Utility, Management
**License:** AGPL-3.0-only (custom link to the LICENSE file on GitHub)
**Source:** https://github.com/GoatTech-42/meshac
**Issues:** https://github.com/GoatTech-42/meshac/issues
**Game versions:** 1.21.11, 26.1.x, 26.2, 26.3 (one jar per version: meshac-1.21.11.jar needs Java 21, the others need Java 25)
**Dependency:** Fabric API (required)
**Icon:** assets/icon-512.png
**Gallery:** assets/banner.png as the featured image (new tagline), plus the screenshots listed at the bottom

## Description

meshac is an anticheat that lives inside your Fabric server. Nobody has to install anything on their end.

The bar for meshac is simple: if a hack can be seen from the server, catch it and stop it, and stay quiet on someone who is just playing. That bar is a goal, not a promise: false flags remain. Current limits include Bedrock and linked accounts, and ordinary movement and interaction cases still under review. Flags are reviewable in game, and monitor mode lets you watch before it ever kicks.

**What it watches**
- Movement: flight, speed, high jump, step, glide, timer, no-fall, no-clip, boat fly, anti-knockback
- Combat: kill aura, reach, criticals, attack rate, attribute swapping (log-only with monitor mode on - see below)
- Building and interaction: fast place, clicks that are not in your line of sight, and loud scaffold and auto-build patterns (what gets through is listed under honest limits)
- Inventory and mining: fast break, nuker, impossible inventory actions

**How it punishes**
It does not react to one weird packet. Every flag adds heat to a player, and heat cools off on its own. Several independent things have to go wrong before anyone is removed, so a lag spike or a laggy tunnel batching packets does not look like a cheat. Two strong flags in a row, like real flight, remove a player fast.

Removal is a ladder: kick first, then a tempban that grows each time (10 minutes, 1 hour, 6 hours, 36 hours, up to 7 days). Old offences fade over time. Permanent bans are off by default.

Monitor mode is OFF by default: the config field ships unset (Config.java:31 declares `public Boolean monitorOnly;` with no initializer and no bundled default config sets it; Config.java:75 returns true only when it is set to true), so out of the box every check enforces (Config.enforce = !monitor(), Config.java:52) - including combat. `/mesh monitor on` (or monitorOnly: true in the config) makes every detector log and alert without kicking or banning. This release is tested and shipped for monitor mode on.

Combat with monitor mode on is the exception in this release: combat signals (kill aura, reach, criticals, attack rate, attribute swapping) are log-only. They record, alert staff and build review evidence, and with monitor mode on they never remove anyone - and monitor mode is the posture this release is tested and shipped for. Combat thresholds are unchanged from the tested build; any change to them, or turning combat punishment on, is gated on a false-flag replay at 100 ms and 300 ms ping plus an independent review.

**Staff tools (in game, op only)**
- /mesh cases and /mesh case <id> to see what happened and why
- /mesh status <player> for a player's heat, offences and what happens next
- /mesh watch to get flags in chat as they happen
- /mesh monitor on|off to log everything without kicking anyone
- /mesh kick, /mesh ban, /mesh pardon, /mesh unban, /mesh reload

There are three presets (lenient, default, strict) and every number can be overridden in the config. There is an optional Discord webhook for holds and removals.

**Honest limits**
Some hacks look exactly like legit play from the server side (a client that only sprints at normal speed, for example). meshac does not pretend to catch those. Combat: with monitor mode on, kill aura, reach, criticals, attack rate and attribute swapping flags are log-only - no removals come from combat flags in that posture, and combat detection has no rig-tested punishment evidence yet; treat combat flags as review leads, not proof. A scaffold or auto-builder that turns its view before placing, keeps a block in hand and stays under about 4 blocks a second is a predicted miss based on source reading, not rig-verified. A proposal explores this gap; nothing is shipped for it. Test results, misses included, are in docs/coverage.md in the repo.

Detection logic is exercised against our own test client (meadow) and a scripted bot rig on an isolated server, plus a legit-play suite that has to stay at zero flags. Earlier builds were also run against Wurst on the 26.1.2 test rig (notes in docs/coverage.md). Combat rows on the current build are drafted but not run yet.

## Screenshots to take (real in-game, from the test server)
1. The kick screen a removed player sees
2. /mesh cases output in chat
3. /mesh status on a flagged player
4. /mesh watch flag feed during a test

Status: draft only. Screenshots 1-4 still need to be captured on meshac-lan. Nothing is published.

## v3 verification notes (reviewer round B, 10-17)

1. Every combat log-only claim now carries the monitor-mode-on condition: Summary
   ("combat (log-only in monitor mode)"), the What-it-watches combat bullet, the "How it
   punishes" combat paragraph ("Combat with monitor mode on is the exception"), and the
   Honest-limits combat entry ("with monitor mode on ... in that posture").
2. Monitor-mode default stated from the repo: OFF by default. monitorOnly is a null-default
   Boolean (Config.java:31 `public Boolean monitorOnly;` - no initializer), monitor() returns
   true only when set (Config.java:75 `return monitorOnly != null && monitorOnly;`), and no
   bundled default config sets it (src/main/resources holds only fabric.mod.json and
   meshac.mixins.json). Consequence stated in the listing: combat ENFORCES out of box
   (Config.enforce = !monitor(), Config.java:52); the log-only posture requires
   `/mesh monitor on` or monitorOnly: true; the release is tested monitor-on.
3. Summary re-verified within the 256-char field limit (length check run in the patch script).

## v2 verification notes (review agent points 1-5, 10-17)

1. Combat carve-out added under "How it punishes". GROUNDING CAVEAT for the reviewer: by
   construction d124 enforces combat when monitor mode is OFF (Config.enforce = !monitor()).
   "Log-only" is the tested/release posture (W2 rule), not a code guarantee; the by-construction
   binding is the DRAFT-only proposal in rig/COMBAT-LOGONLY-BY-CONSTRUCTION-PROPOSAL.md (not
   shipped). The wording says "with monitor mode on" to stay true.
2. Honest-limits combat entry added (log-only, no removals, no rig-tested combat punishment
   evidence; nb-cbt rows are DRAFT/unrun).
3. Summary combat bullet kept with the log-only qualifier, trimmed to fit the field: the
   first pass ran 301 chars (over the 256 limit); the shipped wording above is 249 chars,
   within it (the 267-char middle pass was caught and re-trimmed before delivery). "Combat (log-only this release)" carries the qualifier inline.
4. "Wurst and Meteor" replaced: the series evidences meadow (our own client) on the rig;
   coverage.md documents Wurst runs on the 26.1.2 rig (:78, :84); Meteor appears only in an
   UNTESTED list (coverage.md:39), so Meteor is dropped.
5. Verified against the repo: all ten listed /mesh subcommands exist in MeshCommands.java
   (cases, case, status, watch, monitor on|off, kick, ban, pardon, unban, reload); the repo
   also has capture start/clear/dump, veil xray|esp|off and check subcommands the listing
   omits (choice, not error). Ladder numbers match the default-preset computed ladder
   (1 kick, then 10/60/360/2160 min capped at 10080 min = 7 days; Config.tempBase=10,
   growth=6, max=10080, kicks()=1); lenient/strict presets differ and the draft says numbers
   are overridable. Permanent bans off by default (Config.perma() default false). Version/Java
   claims match versions/*.properties (1.21.11 java 21; 26.1.x/26.2/26.3 java 25). LICENSE is
   GNU AGPL v3 ("AGPL-3.0-only" consistent).
