# Modrinth listing draft (not published)

**Name:** meshac
**Slug:** meshac
**Summary (under 256 chars):** A native Fabric anticheat. Runs on the server, players install nothing. Watches movement, combat, building, inventory and mining, and removes real cheaters without flagging legit play.

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

I built it because most anticheats either miss things or ban honest players for lagging. The bar for meshac is simple: if a hack can be seen from the server, catch it and stop it, and never touch someone who is just playing.

**What it watches**
- Movement: flight, speed, high jump, step, glide, timer, no-fall, no-clip, boat fly, anti-knockback
- Combat: kill aura, reach, criticals, attack rate, attribute swapping
- Building and interaction: scaffold, fast place, auto build, clicks that are not in your line of sight
- Inventory and mining: fast break, nuker, impossible inventory actions

**How it punishes**
It does not react to one weird packet. Every flag adds heat to a player, and heat cools off on its own. Several independent things have to go wrong before anyone is removed, so a lag spike or a laggy tunnel batching packets does not look like a cheat. Two strong flags in a row, like real flight, remove a player fast.

Removal is a ladder: kick first, then a tempban that grows each time (10 minutes, 1 hour, 6 hours, 36 hours, up to 7 days). Old offences fade over time. Permanent bans are off by default.

**Staff tools (in game, op only)**
- /mesh cases and /mesh case <id> to see what happened and why
- /mesh status <player> for a player's heat, offences and what happens next
- /mesh watch to get flags in chat as they happen
- /mesh monitor on|off to log everything without kicking anyone
- /mesh kick, /mesh ban, /mesh pardon, /mesh unban, /mesh reload

There are three presets (lenient, default, strict) and every number can be overridden in the config. There is an optional Discord webhook for holds and removals.

**Honest limits**
Some hacks look exactly like legit play from the server side (a client that only sprints at normal speed, for example). meshac does not pretend to catch those. Test results, misses included, are in docs/coverage.md in the repo.

Built and tested against real hacked clients (Wurst and Meteor) on an isolated test rig, plus a legit-play suite that has to stay at zero flags.

## Screenshots to take (real in-game, from the test server)
1. The kick screen a removed player sees
2. /mesh cases output in chat
3. /mesh status on a flagged player
4. /mesh watch flag feed during a test

Status: draft only. Screenshots 1-4 still need to be captured on meshac-lan. Nothing is published.
