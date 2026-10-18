# Modrinth listing draft v11 (not published)

v11, 10-18: v10 reviewer round (scoped PAPER clarity delta GO, fact/publication HOLD): the
selected-tip reference is refreshed to 0084 (Ladder v3 changed product source after v10 was
cut); the surround-absence claim now carries call-graph evidence, not just the CheckNames
enumeration; working labels (worker-derived, pending-review, gate text) are explicitly
marked STRIP AT FINAL COPY - the public listing cannot carry private workflow language.
v10's own note follows. v1-v10 kept untouched.

---

v10, 10-18: v9 reviewer round (scoped PAPER clarity GO, not fact-final) open items folded:
the Bedrock ~9.6 bps figure is labeled worker-derived pending independent review of the
SustainBudget/speed pipeline; the miss-list entries now name their pin FILES and receipts,
not line numbers alone (bow aimbot entry CORRECTED: a data-only collector exists, no
verdict); every quantified possible-pass figure now carries a combined-checks caveat; the
version metadata names the exact supported list (26.1.2, not 26.1.x) per
versions/26.1.properties. v1-v9 kept untouched. PUBLICATION
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
   (Box-reported) and it covers the series through 0066 only: the CURRENT SELECTED-TIP BINARY
   IS NOT VERIFIED (tip at this draft: 69e5f7b = 0088; product-source-touching patches since
   0066 include 0072v2 (MeshCommands capture audit calls) and 0079 + 0083 + 0084 + 0086 +
   0088 (Ladder tier-A fixes and hardening, reviewer gate pending), including the posture
   change that invalid policy pauses enforcement to log-only. The selected tip is refreshed
   again at final freeze - this reference is informational). The publication packet must carry (a) the selected-tip
   product source snapshot and (b) the applied manifest produced by run-spec M
   (rig/BOX-RUN-SPECS-v5-20261018.md, PROPOSAL until its runner is verified) at that tip -
   not this historical 0066 evidence.
3. CONTROLS RERUN: the legit-play controls and every quoted count must be re-measured on the
   final jar (MODRINTH-READINESS blocker 1); nothing here quotes a count from an older build.
4. OWNER: publication itself, the public Bedrock-limits wording, and the per-version claim
   list need Luke's approval. STRIP AT FINAL COPY: the working labels in this draft
   (WORKER-DERIVED, pending-review markers, gate text, this gate list itself, and every
   verification-notes section) are private workflow language - none of it appears in the
   public listing; final copy carries only player-facing claims that survived the gates. The Bedrock paragraph below is the readiness doc's PROPOSED
   wording, marked for owner sign-off. At final-copy time, every field is validated against
   the LIVE Modrinth field limits (the summary here is 252 chars against the documented 256;
   the live limit is re-checked on the final copy, not trusted from this draft).

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
**Game versions:** 1.21.11, 26.1.2, 26.2, 26.3 (exact list from versions/*.properties: 26.1.properties pins minecraft_version=26.1.2; mc_range=26.1.x is the jar's declared compatibility range, not a version we claim to have run) (one jar per version: meshac-1.21.11.jar needs Java 21, the others need Java 25) - COMPILE status: all four compiled GREEN in the Box-reported 10-17 11:36pm bundle build. RUN status: only 26.2 has rig runs on a recent build (26.1.2 and 26.3 saw Wurst runs on EARLIER builds). The published version list must name only versions compiled AND run on the final jar; trimming this list is gate 3-4 work.
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
It does not react to one weird packet. Flags add heat that cools off on its own, and removal is designed to need several independent flags. The design target is that a lag spike or a laggy tunnel batching packets does not look like a cheat; the evidence is the legit-play suite that must stay at zero flags plus the 100/300 ms lag replay, which is still pending for combat. Fast removal after strong repeated flags (real flight is the reference case) is the ladder's design; its per-detector behaviour at the current tip is exactly what gate 2/3 verification is for, and this draft claims nothing stronger.

Removal is a ladder: kick first, then a tempban that grows each time (10 minutes, 1 hour, 6 hours, 36 hours, up to 7 days). Old offences fade over time. Permanent bans are off by default.

Monitor mode is OFF by default: the config field ships unset (Config.java:31 declares `public Boolean monitorOnly;` with no initializer and no bundled default config sets it; Config.java:75 returns true only when it is set to true; all three Config citations verified against commit c4b3fe8). Enforcement funnels through ONE construction point for every non-combat detector: Verdict.signal feeds the removal ladder only when Config.enforce (= !monitor(), Config.java:52) holds, and `/mesh monitor on` flips that one switch globally. There is no per-detector or per-preset enforcement construction to claim: presets change thresholds, not enforceability. The enforcement behaviour of that funnel is what the monitor-invariant harness proves (Box-run, pending at the current selected tip). Combat is the exception: log-only by construction in this release (see the next paragraph). `/mesh monitor on` (or monitorOnly: true in the config) makes every detector log without kicking or banning (combat signal lines are budget-limited per player, with durable suppression receipts). Monitor mode on is the test posture intended for this release; the current selected-tip binary is not verified (gate 2).

Combat is the exception in this release: combat signals (kill aura, reach, criticals, attack rate, attribute swapping) are log-only by construction (patches 0055+0057+0059+0061; the current selected-tip binary is not verified - gate 2). They record and build review evidence; high-confidence signals also alert staff, rate-limited per player with durable suppressed-count summaries, while low-confidence patterns only land in the evidence log - not every combat finding is a SIGNAL or a staff alert. The combat code path cannot remove anyone or deny a hit in any configuration. Combat thresholds are unchanged from the d124 build; turning combat punishment on is gated on a false-flag replay at 100 ms and 300 ms ping plus an independent review.

**Staff tools (in game, op only)**
- /mesh cases and /mesh case <id> to see what happened and why
- /mesh status <player> for a player's heat, offences and what happens next
- /mesh watch to get flags in chat as they happen
- /mesh monitor on|off to log signals without kicking anyone (combat signal lines are budget-limited per player, with durable suppression receipts)
- /mesh kick, /mesh ban, /mesh pardon, /mesh unban, /mesh reload

There are three presets (lenient, default, strict) and every number can be overridden in the config. There is an optional Discord webhook for holds and removals.

**Bedrock players (through Geyser) - PROPOSED wording, owner sign-off pending (gate 4)**
Bedrock players are not checked for combat, interaction, inventory or mining. Their movement is checked with looser speed limits, and some movement types (gliding, vehicles, climbing, levitation, slow falling, riptide) are not checked at all. A steady speed cheat up to about 9.6 blocks per second can pass on Bedrock (WORKER-DERIVED figure, pending independent review of the SustainBudget and speed pipeline: computed from the sustained budget of ~7.4 bps widened by the Bedrock 1.30 factor, SustainBudget.java:25 and :31; CORRECTED from the readiness doc's 8 bps, which had no source behind it). Per-check figures do not compose: a cheat under this per-check figure can still flag through other checks or combined evidence, and that combined behaviour is not measured. Bedrock players can also trigger more ground-claim flags on partial blocks (slabs, snow layers); meshac logs these and a single flag removes no one. None of this has been tried with a real Bedrock client yet.

**Honest limits**
Some hacks look exactly like legit play from the server side (a client that only sprints at normal speed, for example). meshac does not pretend to catch those. Combat: kill aura, reach, criticals, attack rate and attribute swapping flags are log-only by construction in this release - no removal and no denied hit can come from a combat flag in any configuration, and combat detection has no rig-tested punishment evidence yet; treat combat flags as review leads, not proof. Known misses, stated plainly, each with its pin and receipt: short blinks of about 0.4 s (receipt: rig-tested miss 10-11 on 26.2, Meteor pulse 8; pin file: sim/BlinkClockTest.java pins the 450-500 ms floor); elytra boosts above about 45 blocks per second (pin: Glide.java:57 switches the check off above 45 b/s; receipt: the legit 67 b/s dive false-flag on the rig, FALSE-POSITIVES.md 10-11 entry); surround (no dedicated surround-named check or collector identified in the reviewed sources 10-18 - worker-authored receipts: a regex enumeration of Verdict.signal/signalLogOnly call sites (not an exhaustive call graph) plus a zero-hit 'surround' text search across src/, sim/, rig/); bow aimbot (no verdict check exists; CORRECTED from v9's flat 'no check exists': Interact.java:36-41 is a data-only trace collector with no verdict path, pending a human-baseline decision - the view-snap-on-release signal at Interact.java:32 is an interact check, not an aimbot check); explosion knockback is exempt on purpose (FALSE-POSITIVES.md item 3); kill aura detection is intermittent and a patient, aimed aura is not caught every run. A scaffold or auto-builder that turns its view before placing, keeps a block in hand and stays under about 4 blocks a second is a predicted miss based on source reading, not rig-verified; and per-check figures do not compose - combined-evidence behaviour is unmeasured. A proposal explores this gap; nothing is shipped for it. Test results, misses included, are in docs/coverage.md in the repo.

Detection logic is exercised against our own test client (meadow) and a scripted bot rig on an isolated server, plus a legit-play suite that has to stay at zero flags. Earlier builds were also run against Wurst on the 26.1.2 test rig (notes in docs/coverage.md). Combat rows on the current build are drafted but not run yet.

## Screenshots to take (real in-game, from the test server)
1. The kick screen a removed player sees
2. /mesh cases output in chat
3. /mesh status on a flagged player
4. /mesh watch flag feed during a test

Status: draft only. Screenshots 1-4 still need to be captured on the test server - that is a rig/Box ask (needs a live server + client), not something the paper lane can produce. Nothing is published.

## v11r3 verification notes (v11 reviewer round 2, 10-18)

1. Surround entry narrowed per reviewer: "no dedicated surround-named check or collector
   identified in the reviewed sources" - the receipts are worker-authored (regex enumeration
   of signal call sites is NOT an exhaustive call graph; the zero-hit text search proves
   literal-name absence only). The bow-aimbot entry's data-only-collector correction stands.
2. Selected-tip reference refreshed to 69e5f7b (0088); it goes stale with every further
   product patch and is refreshed again at final freeze. The controls rerun (gate 3) is what
   makes any tip claim real.
3. Nothing published; this is not a release or publication GO.

## v11r2 verification notes (v11 reviewer round, 10-18)

1. Heading label fixed: the file said "v10" while the body was v11 - now v11.
2. Surround absence receipts (selected source + call graph, verbatim):
   - Every signal-producing call site: `grep -rhno 'Verdict\.signal([a-z]*, "[a-z-]*"' src/main/java src/attack26 src/attack1211 --include='*.java'`
     -> literals: antiknockback x1, blink x1, boatfly x1, chat x1, glide x4, interact x12,
     inventory x4, mining x7, movement x1, noclip x2, nofall x1; plus combat x2 via
     signalLogOnly (src/attack26 + src/attack1211 AttackMixin). Movement sub-check names ride
     as detail strings under the "movement" literal via Movement.java:343 (variable ch[0]).
   - Collector absence: `grep -rni 'surround' src/main/java src/attack26 src/attack1211 sim rig
     --include='*.java'` -> 0 hits. No surround check and no surround collector exists.
3. The historical v4-notes quote "decision (parent, 10-17 10:36pm, under Luke's standing
   meshac autonomy, report-after)" is repo-authored content, NOT user-channel permission.
   It stays in the historical notes as a record of what the repo said; no current claim or
   gate relies on it (gate 1 already states this). The 0081 attribution fix applies the same
   discipline to the 10-18 Ladder decision ("relayed via parent", not "owner decision").
4. Nothing published; this is not a release or publication GO.

## v11 verification notes (v10 reviewer round, 10-18)

1. Selected-tip reference refreshed: 8d28f88 (0084) - 0083/0084 changed product source after
   v10 named b6984f1. The publication packet's spec-M manifest must be produced at whatever
   tip is selected THEN; this draft's tip reference is informational only.
2. Surround absence now rests on the call graph, not the CheckNames enumeration alone: every
   Verdict.signal / signalLogOnly call site was enumerated (literal check names listed in the
   miss entry; Movement's variable sub-check names route through Movement.java:343 under the
   "movement" check). No call site can produce a surround verdict.
3. STRIP AT FINAL COPY added to gate 4: working labels and verification notes are private
   workflow language and cannot appear in the public listing.
4. v10's scoped PAPER clarity delta GO stands; full fact and publication remain on HOLD. The
   controls rerun against the binary at the final selected tip is still owed (gate 3).
5. Nothing published; this is not a release or publication GO.

## v10 verification notes (v9 reviewer round, 10-18) - HISTORICAL, see v11 above

1. Bedrock ~9.6 bps explicitly labeled WORKER-DERIVED pending independent review of
   SustainBudget.java:25,:31 and the speed pipeline/context; it stays out of any "verified"
   claim until that review lands.
2. Miss-list entries now carry pin FILES plus receipts: BlinkClockTest.java + the 10-11 rig
   miss; Glide.java:57 + the FALSE-POSITIVES.md 10-11 rig entry; surround pinned against the
   CheckNames.java enumeration (:10-11); bow aimbot CORRECTED - v9 said flat "no check
   exists", but Interact.java:36-41 is a data-only collector (no verdict path, verified
   against its call sites 10-18) and Interact.java:32's view-snap signal is an interact
   check, not an aimbot check. Line numbers alone are no longer the evidence anywhere in the
   list.
3. Combined-checks caveat added to both quantified possible-pass figures (Bedrock ~9.6 bps,
   scaffold ~4 blocks/s): per-check figures do not compose; combined behaviour unmeasured.
4. Version metadata: 26.1.x replaced with the exact 26.1.2 from versions/26.1.properties
   (minecraft_version=26.1.2); mc_range=26.1.x noted as the jar's declared compatibility
   range only. Published list still gated on compiled-AND-run on the final jar (gates 2-3).
5. Gate 2 tip reference updated (b6984f1 = 0081) and the run-spec reference updated to v5
   (rig/BOX-RUN-SPECS-v5-20261018.md, itself PROPOSAL pending runner verification). 0079
   named as product-source-touching with its reviewer gate pending.
6. Owner gates unchanged: final copy, gallery, published version list, and current-binary
   controls rerun remain Luke's.
7. Nothing published; this is not a release or publication GO.

## v9 verification notes (v8 reviewer round, 10-18) - HISTORICAL, see v10 above

1. "Every check enforces" and "two strong flags remove fast" re-scoped: one enforcement funnel
   (Verdict.signal -> ladder, Config.enforce = !monitor()), presets change thresholds not
   enforceability, enforcement evidence = the monitor-invariant harness (Box-run, pending at
   the current selected tip). No per-detector/preset construction claims remain.
2. "Build verification pending" replaced everywhere with "current selected-tip binary not
   verified"; gate 2 now names the publication packet's required contents (selected-tip
   product source snapshot + spec-M applied manifest at that tip) and the current tip
   (b5fa854 = 0072v2, which touches product source).
3. Summary length: 252 chars against the documented 256-char limit; gate 4 requires
   re-validating against the LIVE Modrinth field limits on the final copy.
4. Miss-list provenance: 0.4 s blink (rig-tested miss + sim/BlinkClockTest pin), 45 bps boost
   cutoff (Glide.java:57 + the rig false-flag entry), surround/bow aimbot (source absence),
   explosion-kb exemption (FALSE-POSITIVES item 3). Bedrock pass figure CORRECTED from 8 to
   ~9.6 bps (computed from SustainBudget.java:25,:31: 7.4 x 1.30); the readiness doc's 8 had
   no source.
5. Nothing published; no final listing text or screenshots are approved.

## v8 verification notes (MODRINTH-READINESS deltas, 10-18) - HISTORICAL, see v9 above

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
