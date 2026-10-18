# Candidate9 integration

Base89c8625. Branch worker-candidate9-integration-20261016, isolated RAM worktree
/dev/shm/meshac-candidate9-integration-20261016. Pure source integration only;
candidate8Box snapshot untouched. No build/deploy/config/client/world mutation.

## Frozen inputs in requested order

1. Action audit9bed5ca+150bb76 ->508897a+a373590.
2. Movement edgesim8b14d11 ->6cfa169.
3. Veil worktree observed tip9b54d4c. Chain33f94ed,f3bd305,b91acb1,eb6d02f,
   f1fe6d2,9b54d4c ->88afd45,e4b1e3d,e3f5fca,df121f6,84505fc,a030180.
4. Docs chainaa5b0f8,2831967,7b3d1a8,9f62ac2,728730b ->610e6ed,999c89a,
  6eca06c,193c90c,3ca7b90.

## Resolution / semantic checks

Only actual merge conflicts were additive sim/run-tests.sh compile+test lists,
deduplicated union preserving every input. MeshCommands/Mining auto-merged;
reviewed Mining retains server-progress weighted rate+trace and delegates old
early-stop arithmetic to pure DigRule. Fixed audit source-location assertion to
check both DigRule caller and unchanged formula in its new file, not delete the
assertion. Docs38test count labelled historical rather than claim current count.
No other policy choice or wholesale side-selection. Current combat observedLOS,
TRACE_FULL, distinct-tick/history policies preserved. No research clock wiring.

Veil border PlanGuard is now wired by branch request. HideRule and ExemptRule
are active source changes and dropGoneHidden lifecycle handling is included.
Operator guide absolute no-ban claim is corrected: indirect interactions require
native proof. Verify API/native semantics; pure suite alone doesn't validate the
Minecraft adapters. Edge sim is a calibrated toy model, not native FP proof.

## Results

46registered Java pure classes PASS once on JDK25nice19/RAM scratch, including
new ActionEvidence,MiningRate,ActionTraceBudget,LegitEdgeSim,PlanLifecycle,
MiningReveal,HideRule,VeilSituations. All14rig/test*.py entrypoints pass finally.
TCPproxy loopback test initially FAILED release250ms observation (0bytes), then
immediate unmodified rerun4testsPASS. Do not call this stable: timing-sensitive
harness needs follow-up, retains socket/pipe ResourceWarnings. Other13entrypoints
passed first run. No game rig/process involved. git diff --checkclean.

Native build/API/version checks go to Box next. FP1105/1107, actual legal damage/
placement/sort/break proofs, veil-on shared heat, launch/piston/slime and40player
CPU/GC remain gates. Exact opt-in ACTION_TRACEis off by default, bounded, log-only.
Source/pure results are neither deployment authority nor release/FP clearance.
