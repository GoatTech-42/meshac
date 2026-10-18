# Candidate9c Bedrock compare diagnostics

Base9c810ef(candidate9b). No enforcement/cap changes. Exact opt-in compare stays
off by default. Shadow balance resets alongside every real spBal zero-reset,
including blink, init/teleport, hold, real budget crossing, LOG and correction.
Exemption halving retains parity. Diagnostic cooldown/suppression history is not
cleared by motion resets, so corrections cannot bypass diagnostic rate limits.

CompareLogBudget: server-thread-owned global4lines per20tick fixed window, caller
one line per100ticks. Clock comes from server tick, not wall time. Caller state
lives in Movement.S, no UUID registry. New connections may bypass caller cooldown
but not global limit. Tick rollback resets windows. Counters saturate at Long.MAX.
Every suppressed threshold crossing counts, even when shadow balance resets;
next permitted line includes cumulative caller+global suppressions. No trailing
summary is guaranteed if no later crossing emits, so silence isn't zero events.

At20TPSglobal max240lines per1200tick window, rather than40*12=480/minute.
Fixed windows can allow8lines across a boundary, not a rolling4cap. A single tick
allows at most4except tick rollback; no unbounded reconnect burst. No Strings
formatted on suppressed crossings. Extra constant-time arithmetic/counters only.
CPU/GC cost unprofiled. Raw MESHAC_TRACEand other logger paths are separate and
not covered by this bound.

49pure Java classes PASS,4new Python wiring tests PASS. Tests pin every6real
reset sites, halving parity, no Verdict mutation, tick-time budget; pure budget
covers40players, duplicate ticks, exact boundaries, suppressed caller eligibility,
100tick caller cooldown, cumulative counts and tick rollback. Native version/API
compile and Bedrock execution proof remain Box/rig gates. No deployment/build/
rig change. Previously observed proxy timing flake remains unrelated and open.
