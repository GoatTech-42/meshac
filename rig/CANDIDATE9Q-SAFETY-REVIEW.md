# Candidate9q monitor safety review: integration segment

Base9p b253252a87390f9100f11a45f23480744c53bba7. This segment covers production-r3 missing guards1/2/3/7/8 plus feature switches. Movement guards4/5/6 are owned by the movement worker and MUST be combined before final9q freeze. This historical segment was subsequently combined with all movement guards; see final integration below. Source validation alone is not release clearance.

## Evidence and changes
- aa6c029 and3f313c6: attack1211 lines35-38 and attack26 lines26-30 called Combat.check, then Verdict.signal, then unconditional cancel. Both call sites originate1396ef8, and shared-ref git log --all -S enforce("combat") has no hits for these files. Box inspected separate live production jar with an enforce guard: live bytecode and repository source differ; this is not proof the live jar is unsafe or9m caused the bug.
- Both AttackMixin versions now capture Verdict.Step and cancel only !=LOG. No duplicate hook.26.x uses ServerboundAttackPacket/handleAttack;1.21.11 uses interact dispatch.onAttack.
- Config.enforce is !monitor(), with no egregious bypass. Existing keys/unknown file settings retained; legacy egregiousChecks cannot override monitor. Verdict preserves exempt/disabled and separate monitor heat but all monitor signals return LOG. Final remove checks enforce before case creation/disconnect. clearPendingActions can clear detector heat for movement worker toggle/reload integration.
- Cases.activeBan skips automatic meshac/meshac-fingerprint cases during monitor and continues scanning, so an older staff ban remains effective.
- Fingerprints monitor branch logs at most once/5s/player, creates no case/disconnect/REMOVED marker and clears LOG_AT on forget.
- Flat config feature keys: veilEnabled master default true with existing veilXray/veilEsp default false; enderchestEggRule default true preserves prior rule. Both can be turned off via config+reload. Veil mask/hide/tick gates honor master. Operator doc calls both features not governed by monitor mode. Explicit staff bans/kicks remain intentional staff actions, not detector punishment.

## Tests
-72 pure Java classes PASS and20 rig Python scripts PASS.
- rig/test-monitor-invariant.py compiles actual Cases,Verdict,Fingerprints,Ladder and each actual AttackMixin against instrumented service doubles; copies exact Config.monitor/enforce methods from current source. Both packet families exercise monitor+legacy egregious entries, repeated high-weight verdicts, direct private remove invocation, fingerprint repeated matches/log bound/cleanup, no REMOVED marker, auto/staff ban scan and actual attack callback cancellation. Enforced-mode positives confirm cancel, auto ban and fingerprint removal still work. No Minecraft runtime claims.
- rig/test-combat-logonly-wiring.py has per-version source regressions plus existing raw-denial and feature-switch wiring assertions. No new Minecraft API calls introduced by this segment. Full Fabric/AP/remap and native execution remain Box gates.
- Existing Vehicle/Totem gates, noclip Step.LOG handling, sign enforcement gate, Interact/Mining enforcement-return gates are retained, not replaced with older source.

## Remaining
- Combine movement monitor pending-clear/hold replay/NoFall arming+damage guards and extend single invariant suite to those paths before final freeze. No deployment/rig mutation or heavy build performed here.

## Final combined integration
- Imported full movement chain b5a68c6/f5d26e5/ff6adc2/9ee94ae/fd7d755 onto this branch. Took worker SafeSwap Config.load implementation, with one pending-movement clear and one separate heat clear. No duplicate Config replacement. monitorClear clears all3 action fields at check/tick entry before early returns. Existing server teleport acknowledgment is not a detector punishment.
- All75 pure Java test classes PASS and20 rig Python entry scripts PASS in the final union, including the expanded single monitor-invariant suite and source witnesses. The source fixture baseline comparison has genuine red9p results, not simulated expected failures. Detailed37-row status is in CANDIDATE9Q-MONITOR-ROWS.md.
- Absolute monitor Config/Verdict gates prevent egregious overrides, cases/removal/fingerprints have final guards, and pending hold/NoFall debt clears on monitor transitions/reloads/check/tick. Retained candidate movement/action evidence and context/bounce/block-entity safety changes. No detector was deleted to make a test pass.
- Full Fabric build/AP/refmap/runtime mixin and private rig native trigger witnesses remain required. No deploy, rig jar/config change, process kills or heavy build done by this worker. A final frozen SHA is a source candidate, not GO for ilhsmc.
