# DRAFT patch proposal: powder in the nofall exempt gate (log-only)

Status: PRE-DRAFTED, not a change. Gated on the GC05 battery row (nb-gc05-jb-powder).
Worker recommendation (10-17): DO NOT LAND BEFORE GC05 RUNS. Details at the bottom.

## Finding (FP lane + reviewer, relayed via parent 10-17 7:44-7:46pm)

The nofall window-evidence gate is `ground && !exempt && s.grace <= 0` (Movement.java:320) with

    boolean exempt = baseExempt || pushed || s.boosted;              // Movement.java:172
    boolean baseExempt = pl.isCreative() || pl.isSpectator() || pl.getAbilities().mayfly
        || pl.isPassenger() || pl.isFallFlying() || pl.isAutoSpinAttack() || pl.isInWater()
        || pl.isInLava() || pl.onClimbable() || pl.hasEffect(MobEffects.LEVITATION)
        || pl.hasEffect(MobEffects.SLOW_FALLING);                    // Movement.java:159-162

NO powder entry anywhere in the gate; only the jump/microhop checks gate on powder
(powderNear, e.g. :303). Powder's collision is entity-conditional, so an in-powder ground
claim from a sinking/no-boots player has no collision 0.1 below, reads UNSUPPORTED, and
accumulates window evidence. The only mitigation today is behavioral (vanilla may never send
onGround while sinking/climbing) - UNVERIFIED until GC05 runs on the rig.

## Proposed change (land only if GC05 flags)

Variant A (preferred): treat powder-adjacent claims like classify-mode tolerated claims -
neither add nor evict the window (frozen window, not cleared):

    if (ground && !exempt && s.grace <= 0) {
        if (!claimTol && !powderNear) { ...existing scoring... }
    }

powderNear is the jump check's existing computation, hoisted once and shared (patch 1's
claimAir hoist pattern). Log-only: no verdict change; rate-limited [trace] line per skipped
claim. Witness pins: a GroundEvidence-level sim fixture (powder-adjacent claims leave the
window count frozen) + a source pin on the gate expression.

Variant B (fallback): widen exempt itself (`|| powderNear`). Simpler, but every in-powder
claim CLEARS the window (the exempt/grace branch at :336), laundering accumulated evidence
for a spoofer who dips into powder periodically. Weaker; only if Variant A's freeze semantics
prove wrong in GroundReplay over the GC05 traces.

## Hole analysis (both variants)

A ground spoofer in/near powder gets un-scored claims: nofall is blind within powder reach.
Powder is rare, visible, and stationary - the spoofer must carry and place it; microhop
already accepts the same tradeoff (:304). The hole is worth it ONLY IF GC05 shows the real
client emits the flagging pattern.

## Why not land now

- Window/band are PROPOSALS gated on the zero-FP battery; changing code before the first
  battery number repeats the silent-widening failure mode the reviewer pre-committed against.
- GC05 runs either way (it is a patch-1 GO condition). If green, this patch buys a detection
  hole for zero measured benefit. Precedent: P3-strong's arm level is A-row-gated the same way.
- Cost of waiting is nil: log-only means a red GC05 is data, not player damage.

## Evidence to attach when GC05 decides

GC05 gndclaim/gndclear trace lines + measured struggle dy values (JB I and II), from the
battery run on the patched jar.
