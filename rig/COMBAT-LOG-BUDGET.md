# Candidate6 combined combat log audit and budget

Combined source base: c939c5b +65c15aa +8c08122 (cache cherry-pickf7950ef).
This includes the cache experiment's known dense-fight fail-open coverage trade;
combining it here is not approval to deploy that trade.

## Before

At20attack inputs/second/player, tracing emitted3raw lines per attack:
legacyattack +a15 +feature-raw, approximately3600lines/player/minute before
swap traces. Feature summary every8hits added up to150lines/minute, even with
Trace.OFF. Low-confidence pattern logs had no wall-time cap. Dense20player
fights could therefore exceed75000combat diagnostic lines/minute. Inputs faster
than20Hz could increase this; no fixed attack input ceiling was assumed.
These are source-derived counts, not measured game-server log rates.

## After

Per-player monotonic fixed60second windows:

- Low-confidence evidence:12lines/window. Dropped count is carried on the next
  allowed evidence line as suppressed=N; no separate dropped-message log.
- Feature summaries:6lines/window.
- ALL combat raw trace families share120lines/window: swap timeline, legacy
  attack, a15 andfeature-raw. Trace.ON remains required. A category cannot create
  extra windows by changing target or pattern text.

Total normal diagnostics at most18lines/player/window; tracing at most138.
20players:360normal or2760with tracing per fixed window. Any arbitrary sliding
minute can straddle two fixed windows, so its safe upper bound is double those
figures. These are combat diagnostics only, not Movement/Veil/server-wide logs.
Existing geometric enforcement Verdict logging outside Combat.java is not capped
by this class. No new config key or unlimited bypass was introduced.

No observation is used as a punishment due to logging. Feature counters and A15
history still update when logs are suppressed. Suppressed raw trace/summary
samples are not persisted elsewhere; this changes diagnostics and must be stated
when designing a capture. The shared raw cap can exhaust before the end of a
long fight and doesn't guarantee equal sampling between trace families. Do not
infer a full trace or cadence distribution from the remaining log alone. Keep
capture cases short or design a separate explicit bounded structured recorder.
Evidence dropped counts are exact up to saturatingLong.MAX_VALUE; summary/raw
counts are intentionally not additional log lines.

Cache does not log per entity or cache miss. Feature windows still bounded64;
cache8entries/player/tick. No per-target/pattern rate-limiter maps or timers.
State clears on disconnect. Targets, weapons and fight gaps cannot reset log caps.

## Tests

CombatLogBudgetTest simulates60000attempts/channel: exactly12/6/120accepted,
dropped-evidence count, drain, no early reset, exact boundary, clock reset and
player isolation. Whole pure suite+3feature wiring+8candidate6guards pass.
No full Fabric build, native clients, config changes or actual log-volume capture.
