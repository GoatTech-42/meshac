# Modrinth listing draft v8 (not published)

v8, 10-18: applies the MODRINTH-READINESS audit deltas to v7 and re-grounds the gate. PUBLICATION
GATE (all four must hold before anything publishes):
1. CONSTRUCTION: the combat log-only-by-construction claim is verified on THIS tree 10-18
   (static, series tip 95233cd): both version-source AttackMixins
   (src/attack26/.../AttackMixin.java and src/attack1211/.../AttackMixin.java) are
   non-cancellable and route every combat hit to Verdict.signalLogOnly, which never feeds the
   removal ladder in any config. NOTE on provenance: the "decision (parent, 10-17 10:36pm)"
   cited in the 0055 commit message and v7 notes is SELF-CITING repo content; it is NOT
   treated as authority here and this draft does not rely on it - the claim stands on the
   code, which any reviewer can re-grep.
2. BUILD: Box-reported 10-17 11:36pm: the delivered 63-patch bundle (0001-0066) applied clean
   onto c4b3fe8, compile x4 GREEN, javac harness GREEN (85 test mains, exit 0). Second-hand
   (Box-reported), and it covers the series through 0066 only: 0067-0069v3 are
   tooling/docs/fixtures with no product-code change, but per the 0069v3 banner rule the run
   review must require a CURRENT full apply manifest at the tip under review, not this
   historical one.
3. CONTROLS RERUN: the legit-play controls and every quoted count must be re-measured on the
   final jar (MODRINTH-READINESS blocker 1); nothing here quotes a count from an older build.
4. OWNER: publication itself, the public Bedrock-limits wording, and the per-version claim
   list need Luke's approval. The Bedrock paragraph below is the readiness doc's PROPOSED
   wording, marked for owner sign-off.

The monitor paragraph, combat paragraph and staff-tools text are unchanged from v7 (still
accurate against the verified tree). v1-v7 kept untouched. Verification notes at the bottom.

**Name:** meshac
**Slug:** meshac
**Summary (under 256 chars):** A native Fabric anticheat. Runs on the server, players install nothing. Watches movement, combat (log-only by construction), building, inventory, mining. Built to limit false flags on normal play, with a log-only monitor mode so you can check it first.

**Project type:** Mod
**Loader:** Fabric
**Environment:** Server-side required, client-side not needed
**Categories:** Utility, Management
**License:** AGPL-3.0-only (custom link to the LICENSE file on GitHub)
**Source:** https://github.com/GoatTech-42/meshac
**Issues:** https://github.com/GoatTech-42/meshac/issues
**Game versions:** 1.21.11, 26.1.x, 26.2, 26.3 (one jar per version: meshac-1.21.11.jar needs Java 21, the others need Java 25) - COMPILE status: all four compiled GREEN in the Box-reported 10-17 11:36pm bundle build. RUN status: only 26.2 has rig runs on a recent build (26.1.2 and 26.3 saw Wurst runs on EARLIER builds). The published version list must name only versions compiled AND run on the final jar; trimming this list is gate 3-4 work.
**Dependency:** Fabric API (required)
**Icon:** assets/icon-512.png
**Gallery:** assets/banner.png as the featured image (new tagline), plus the screenshots listed at the bottom

## Description

meshac is an anticheat that lives inside your Fabric server. Nobody has to install anything on their end.

The bar for meshac is simple: if a hack can be seen from the server, catch it and stop it, and stay quiet on someone who is just playing. That bar is a goal, not a promise: false flags remain. Current limits include Bedrock and linked accounts, and ordinary movement and interaction cases still under review. Flags are reviewable in game, and monitor mode lets you watch before it ever kicks.

**What it watches**
- Movement: flight, speed, sustained speed (a steady speed-up that stays under the per-move cap), high jump, step, glide, timer, no-fall, no-clip, boat fly, anti-knockback
- Combat: kill aura, reach, criticals, attack rate, attribute swapping (log-only by construction in this release - see below)
- Building and interaction: fast place, clicks that are not in your line of sight, and loud scaffold and auto-build patterns (what gets through is listed under honest limits)
- Inventory and mining: fast break, nuker, impossible inventory actions

**How it punishes**
It does not react to one weird packet. Flags add heat that cools off on its own, and removal is designed to need several independent flags. The design target is that a lag spike or a laggy tunnel batching packets does not look like a cheat; the evidence is the legit-play suite that must stay at zero flags plus the 100/300 ms lag replay, which is still pending for combat. Two strong flags in a row, like real flight, remove a player fast.

Removal is a ladder: kick first, then a tempban that grows each time (10 minutes, 1 hour, 6 hours, 36 hours, up to 7 days). Old offences fade over time. Permanent bans are off by default.

Monitor mode is OFF by default: the config field ships unset (Config.java:31 declares `public Boolean monitorOnly;` with no initializer and no bundled default config sets it; Config.java:75 returns true only when it is set to true; all three Config citations verified against commit c4b3fe8), so out of the box every check enforces (Config.enforce = !monitor(), Config.java:52) - except combat, which is log-only by construction in this release (see above). `/mesh monitor on` (or monitorOnly: true in the config) makes every detector log without kicking or banning (combat signal lines are budget-limited per player, with durable suppression receipts; combat itself is log-only by construction in every config this release). Monitor mode on is the test posture intended for this release; compile and replay verification are pending as of this draft.

Combat is the exception in this release: combat signals (kill aura, reach, criticals, attack rate, attribute swapping) are log-only by construction (patches 0055+0057+0059+0061; build verification pending, see the gate at the top). They record and build review evidence; high-confidence signals also alert staff, rate-limited per player with durable suppressed-count summaries, while low-confidence patterns only land in the evidence log - not every combat finding is a SIGNAL or a staff alert. The combat code path cannot remove anyone or deny a hit in any configuration. Combat thresholds are unchanged from the d124 build; turning combat punishment on is gated on a false-flag replay at 100 ms and 300 ms ping plus an independent review.

**Staff tools (in game, op only)**
- /mesh cases and /mesh case <id> to see what happened and why
- /mesh status <player> for a player's heat, offences and what happens next
- /mesh watch to get flags in chat as they happen
- /mesh monitor on|off to log signals without kicking anyone (combat signal lines are budget-limited per player, with durable suppression receipts)
- /mesh kick, /mesh ban, /mesh pardon, /mesh unban, /mesh reload

There are three presets (lenient, default, strict) and every number can be overridden in the config. There is an optional Discord webhook for holds and removals.

**Bedrock players (through Geyser) - PROPOSED wording, owner sign-off pending (gate 4)**
Bedrock players are not checked for combat, interaction, inventory or mining. Their movement is checked with looser speed limits, and some movement types (gliding, vehicles, climbing, levitation, slow falling, riptide) are not checked at all. A constant 8 blocks per second speed cheat can pass on Bedrock. Bedrock players can also trigger more ground-claim flags on partial blocks (slabs, snow layers); meshac logs these and a single flag removes no one. None of this has been tried with a real Bedrock client yet.

**Honest limits**
Some hacks look exactly like legit play from the server side (a client that only sprints at normal speed, for example). meshac does not pretend to catch those. Combat: kill aura, reach, criticals, attack rate and attribute swapping flags are log-only by construction in this release - no removal and no denied hit can come from a combat flag in any configuration, and combat detection has no rig-tested punishment evidence yet; treat combat flags as review leads, not proof. Known misses, stated plainly: short blinks (about 0.4 s), surround, bow aimbot, and elytra boosts over about 45 blocks per second are not caught; explosion knockback is exempt on purpose; kill aura detection is intermittent and a patient, aimed aura is not caught every run. A scaffold or auto-builder that turns its view before placing, keeps a block in hand and stays under about 4 blocks a second is a predicted miss based on source reading, not rig-verified. A proposal explores this gap; nothing is shipped for it. Test results, misses included, are in docs/coverage.md in the repo.

Detection logic is exercised against our own test client (meadow) and a scripted bot rig on an isolated server, plus a legit-play suite that has to stay at zero flags. Earlier builds were also run against Wurst on the 26.1.2 test rig (notes in docs/coverage.md). Combat rows on the current build are drafted but not run yet.

## Screenshots to take (real in-game, from the test server)
1. The kick screen a removed player sees
2. /mesh cases output in chat
3. /mesh status on a flagged player
4. /mesh watch flag feed during a test

Status: draft only. Screenshots 1-4 still need to be captured on the test server - that is a rig/Box ask (needs a live server + client), not something the paper lane can produce. Nothing is published.

## v8 verification notes (MODRINTH-READINESS deltas, 10-18)

1. Gate restructured into 4 numbered gates (construction / build / controls rerun / owner).
   The construction claim was re-verified on THIS tree: both version-source AttackMixins are
   non-cancellable and call Verdict.signalLogOnly (series tip 95233cd, static grep 10-18).
   The v7 note's "decision (parent, 10-17 10:36pm)" is self-citing repo content - flagged,
   not relied on; the claim needs only the code.
2. Movement bullet gains "sustained speed" (readiness table). The 7.4 bps budget text was
   already in README/coverage; the listing bullet now names the check.
3. Bedrock players paragraph added (readiness proposed wording, owner sign-off pending).
4. Honest limits gains the miss list: short blink ~0.4 s, surround, bow aimbot, boosts over
   ~45 bps, explosion-knockback exemption, intermittent kill aura (from the readiness table
   and the unpublished changelog limits text).
5. Game versions line now separates COMPILE status (Box-reported x4 GREEN 10-17 11:36pm,
   second-hand, series through 0066) from RUN status (26.2 only on a recent build) and gates
   the published list on compiled-AND-run on the final jar.
6. Legit-play softening from readiness blocker 2 was already substantially in v7 ("false
   flags remain", monitor-first posture); no further change needed.
7. README/coverage/ADMIN/FALSE-POSITIVES readiness rows were checked against the CURRENT
   tree 10-18: all already applied (sustained-speed row, fly exemptions, NoFall Bedrock
   caveat, "exempt by design, untested", ADMIN Bedrock movement paragraph + Diagnostics
   section, FP item 4 links). No doc changes owed there.
8. Screenshots stay a rig/Box ask (live server + client required).
9. Still draft only - not a release or publication GO.

## v7 verification notes - HISTORICAL, see v8 above (v6 reviewer repairs, 10-17)

1. Gate + combat paragraph now reference 0055+0057+0059+0061: the durable one-shape
   guarantee waited on the 0061 fix (window-roll receipt loss + suffix leak, now closed).
2. The monitor PARAGRAPH also oversold ("makes every detector log and alert") while the
   bullet was budget-qualified; the paragraph now carries the same budget + receipt wording
   and notes combat is log-only by construction in every config.
3. Still draft only - not a release or publication GO.

## v6 verification notes (v5 reviewer repairs, 10-17) - HISTORICAL, see v7 above

1. Durable-summary claim now follows the corrected code: 0059 makes the guarantee real
   (server-tick flush for idle-connected players, flush-before-clear on the monitor toggle,
   one receipt shape binding name+uuid+check+count+reason). Gate text updated to
   0055+0057+0059.
2. "/mesh monitor on|off to log everything" oversold capped combat logging; now "log signals
   without kicking anyone (combat signal lines are budget-limited per player, with durable
   suppression receipts)".
3. Still draft only - this is not a release or publication GO.

## v5 verification notes (v4 reviewer repairs, 10-17) - HISTORICAL, see v6 above

1. PUBLICATION GATE added at the top: "log-only by construction" is supportable only after
   the 0055-series (0055+0057+0059+0061) closes review AND a build proof exists; today the release jar
   is not verified and this draft may not publish.
2. "tested and shipped for monitor mode on" overstated unrun compile/replay; now states the
   intended posture with verification pending.
3. Absolute claims rewritten: "every flag adds heat / several independent things must go
   wrong / a lag spike does not look like a cheat" are now design-target wording pointed at
   the actual evidence (zero-flag legit suite + the 100/300 ms replay, pending for combat).
4. Staff-alert honesty: combat findings are not all SIGNALs (low-confidence patterns are
   evidence-log only) and staff alerts are rate-limited with durable suppressed-count
   summaries; the combat paragraph says so.
5. The v2/v3 verification notes below are HISTORICAL and superseded: their monitor-on
   condition and "combat enforces out of box" default stopped holding after 0055/0057.

## v4 verification notes (reviewer round on v3 + the by-construction decision, 10-17) - HISTORICAL, see v5 above

1. Contradiction resolved by construction: the v3 combat paragraph said turning combat
   punishment on is gated on FP replay + review while the monitor paragraph said out of the
   box every check enforces including combat. The decision (parent, 10-17 10:36pm, under
   Luke's standing meshac autonomy, report-after): ship combat log-only BY CONSTRUCTION.
   Patch 0055 implements it: both AttackMixins call Verdict.signalLogOnly (never cancel, not
   cancellable), which only records mon heat + a rate-limited SIGNAL line + staff alert and
   always returns LOG - the removal ladder never sees a combat signal in ANY config. The
   listing now says this everywhere instead of the monitor-mode condition.
2. Honest-limits flip side: no longer a default-off exposure - with 0055 the shipped default
   CANNOT remove on combat flags. The entry says so and keeps the no-FP-replay-evidence +
   review-leads honesty.
3. Nit fixed: "unchanged from the tested build" -> "unchanged from the d124 build".
4. Config.java citations (:31 null-default monitorOnly, :75 monitor(), :52 enforce) are no
   longer worker-cited only: Box independently grepped and confirmed them at commit
   c4b3fe8c87a46aa219bcde84109f5c98e13f2cf8 (10-17 10:36pm); the monitor paragraph names that
   commit.
5. Summary field re-verified: 252 chars <= 256 (checked in the patch script).

## v3 verification notes (reviewer round B, 10-17) - HISTORICAL, SUPERSEDED by v4/v5 (monitor-on condition + enforce-by-default no longer hold after 0055/0057)

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

## v2 verification notes (review agent points 1-5, 10-17) - HISTORICAL, SUPERSEDED (see v4/v5 notes)

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
