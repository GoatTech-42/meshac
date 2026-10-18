# Features1,7,10: log-only experiment

Basec939c5b, independent of the los-cache coverage experiment. Source-only.
No combined score, thresholds, Verdict call, denial, heat or punishment exists
in CombatFeatures. Every summary is taggedconfidence=unmeasured,
punishEligible=false. The design note's claims about human behavior are hypotheses.
There is no demonstrated separating signal and no detection clearance.

1. **Target-local aim offset:** signed lateral distance of the view ray's closest
   point from the target centre, in the horizontal eye-to-target radial frame;
   vertical offset normalized by current target height. Rolling64hit variances
   and centred lag1correlation. It is closest-ray geometry, not an actual surface
   impact coordinate. A stationary fixed offset is explicitly legal test data.
7. **Cooldown/gap:** full-charge share, monotonic server handling gap and its
   mean difference from nominal1000/attack-speed milliseconds. This is nominal
   slack, not exact client weapon timing: partial ticks, attribute updates,
   client tick quantization and packet batching affect it. No negative/zero/large
   slack is suspicious by itself. Raw trace also includes attackSpeed/ping.
10. **Tracking:** Pearson correlation of signed lateral ray offset with target
   velocity projected on that same lateral axis. Requires8samples with nonzero
   target lateral velocity and nonzero variance on both dimensions. Stationary
   targets and fixed offsets produce defined=false, never a suspicious zero.
   Positive correlation is a mathematical observation, not an identity claim.

Windows reset on target/weapon-slot change,3second handling gap or nonmonotonic
clock sample. No fight/target mixing. Capacity64; sums are recomputed on the
bounded window rather than maintaining drift-prone unbounded accumulators.
Full raw samples requireTrace.ON. Summaries log every8observations (with enough
same-window samples). Disconnect discards state. No new network packets or rig
configuration are introduced.

These features are different inputs, not proven statistically independent
signals. Features1and10share lateral offset. Do not combine them as three
independent votes, and never promote them to enforcement from this experiment.
Tracking velocity is server-observed current velocity, not a client's view of
that velocity. Rewind/lag matching is still a research question.

## Tests and limits

CombatFeaturesTest covers legal stationary mob-grinder aim, disciplined625ms
full-charge hits, legal rapid weak axe swings, moving tracking covariance,
bunched1ms handling intervals, target/weapon/fight resets, nonfinite inputs and
64sample bounds. Unit tests verify descriptions and no verdict paths, not human
false-positive rates. The real grinder/full-charge/tracking, zoom/controller,
Bedrock, lag and version controls still need native private-rig captures.

No full Fabric build, rig/client use or release. Pure-Java and source wiring
checks only. Existing enforcement geometric checks are untouched by this add-on.
