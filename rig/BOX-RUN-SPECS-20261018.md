# Box run specs (10-18) - SUPERSEDED by rig/BOX-RUN-SPECS-v2-20261018.md (reviewer v1 round: frozen series/targets/toolchain, common run limits, spec S restore + rejoin proof, spec C detector-live + coverage, spec V per-version positive control). Kept for history; do not run from this file.

Scope and safety for ALL four: isolated test rig only (never ilhsmc, never the live server,
no live deploy); fresh throwaway world per spec unless stated; every run attaches its full
server log as the receipt; nothing here authorizes publishing. "Tip under review" = the
series tip at run time (0070 today); per the 0069v3 banner rule every spec that needs a
build starts from spec M (current apply manifest), never the historical 0066 manifest.

## Spec M - current full apply manifest at the tip (prerequisite for 2 and 4)
Objective: prove the current series applies and builds, and produce the manifest of record.
Steps:
1. Fresh clone of meshac main line c4b3fe8 (git archive source of truth, same as 10-17).
2. `git am` the full series 0001 through the tip under review, in order; record each patch's
   apply result (clean / any offset or fuzz).
3. sha256 of the series tar and of every patch file -> MANIFEST.txt + PATCHES-SHA256.txt,
   same shape as the delivered-bundle manifest.
4. `./gradlew` (or the repo's build-all.sh) compile on all 4 version targets; record per-version
   result.
5. Run the javac test harness; record the test-main count and exit code.
Pass: every patch applies clean; all 4 compiles GREEN; harness GREEN with the test count
reported (85 mains was the 10-17 figure at 0066; the count at tip must be reported, any delta
named). Fail: any apply conflict, any compile error, any harness failure - attach the exact
output, stop, report back. Receipt: MANIFEST.txt + PATCHES-SHA256.txt + build log.

## Spec S - listing screenshots 1-4 (needs a live server + client)
Objective: the four gallery shots named in docs/MODRINTH-DRAFT-v8.md.
Build/world: jar from spec M on the 26.2 rig, fresh flat world, one op client + one bot or
second client as the subject.
POSTURE NOTE: shot 1 requires enforcement ON on this isolated rig (monitor off) so a real
removal happens; switch back to monitor on after. Isolated rig only - this never touches live.
Steps:
1. Shot 1 (kick screen): subject runs a scripted flight burst (Meteor flight or the rig's
   flight scenario) until removal; capture the subject client's disconnect/kick screen.
2. Shot 2: op client runs `/mesh cases`; capture the chat output.
3. Shot 3: op client runs `/mesh status <subject>` after the subject has fresh flags; capture.
4. Shot 4: op client runs `/mesh watch`, subject flies again; capture the live flag feed.
Pass: 4 legible PNGs (in-game, UI readable), each named shotN-<what>-<timestamp>.png.
Fail: any shot missing or unreadable - say which and why. Receipts: the 4 PNGs + the server
log segment covering the removals/flags shown.

## Spec C - legit-controls rerun on the final jar (readiness blocker 1)
Objective: every legit-play control at ZERO signals on the jar from spec M, so no listing
text quotes counts from an older build.
Controls (each vs the jar from spec M, 26.2 first; trace on):
1. Sprint-jump course (existing rig control).
2. Wind-charge jumps.
3. TNT and crystal knockback.
4. Elytra: glide from y250, glide with rocket, rocket-free from y90.
5. Hand mining through a tunnel course.
6. Lag proxy walk (rig/h/lagproxy.py, 800 ms freeze every 4 s, 24 s walk).
7. GC01 / GC02 / GC05 per rig/newbounds_controls.py (receipts DRAFT-until-receipt; this run
   is what converts them).
Pass: zero SIGNAL lines across every control; per-control server log captured. Fail: ANY
signal - attach the full [trace] context for it; the control's expected-zero claim is then
BLOCKED and the listing's legit-play wording cannot ship. Receipts: per-control log segments
+ a summary table (control, signals observed, duration).

## Spec V - per-version runs (gate for the published version list)
Objective: run evidence per game version so the listing names only compiled-AND-run versions.
For each of 1.21.11, 26.1, 26.2, 26.3 with its jar from spec M:
1. Boot a fresh flat world; confirm the mod loads (meshac init line in the log).
2. One bot connects; 60 s of walk + sprint-jump.
3. 26.2 additionally: one scripted flight burst as a positive control (must SIGNAL) to prove
   checks are live, not just quiet.
Pass per version: server up, bot connected, zero SIGNALs during legit movement, no crash;
26.2 positive control signalled. Fail per version: boot failure, crash, any legit-movement
signal, or a silent positive control - that version comes OFF the published list until fixed
and re-run. Receipts: per-version server log + the pass/fail table.
