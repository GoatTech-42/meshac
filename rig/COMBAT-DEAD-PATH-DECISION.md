# DECIDED: combat re-arm for 0.1.0 (was: ship-vs-fix decision doc) - 10-17

DECISION (Instinct, under Luke's fully-autonomous standing, relayed by parent 10-17 10:01pm):
OPTION C - 0.1.0 BLOCKS on the combat re-arm. The reach/LOS rules are bound by the binding
rule in W2 (decided by Instinct 10:04pm, NOT a quoted Luke ruling: the earlier "ruling 10a"
reference could not be recovered by either side, so this rule replaces it).

## The gap, grounded in source (cited against the tree at this commit; evidence archive sent with it)

The dead path is DEEPER than the control matrix's "computed, never emitted":
- Combat.check() has NO CALLER on d124. The mixin (ServerGamePacketListenerImplMixin) injects
  move/vehicle/swap/place/sign/use/fall-fly/player-action, but no handleInteract (entity
  attack). The reach (:123), LOS (:135), not-looking (:157) and multi-aura (:195) strings are
  never even COMPUTED.
- The killaura family (cooldown/rate/two-target/snap/swap, inside check() and swapped()) is
  evidence-only by contract: Combat.java:29 "No Verdict call and no return reason: ambiguous
  patterns cannot heat, cancel or punish" and the [combat-evidence] INFO log at :32
  (confidence=low punishEligible=false).
- Combat.tick IS wired (Meshac.java:38, LOS/rewind sampling) and Combat.swapped IS wired
  (mixin :54); Combat.check is the missing link.
- Scoped claim: the ENUMERATED melee paths above are dark. Combat-adjacent signals that fire
  from other checks (attribute swapping via Interact/Inventory, mace smash via Movement) are
  separate paths, not claimed here.
- The listing's combat claims (and coverage.md's AttributeSwap "Held at 10.8 s") rest on the
  26.1.2 rig jar, which had the hook and emitted; d124 has neither.

## Work plan (the re-arm, in order)

W1. Attack hook + emit wiring (0044 draft, scratch-only): inject handleInteract, call
    Combat.check on attack actions; hit strings emit through Verdict ONLY in monitor mode
    (log-only, step ignored - never cancel, never punish); the :29 contract comment rewritten
    in the same patch. Compile gate: Box (no javac in the sandbox).
W2. Reach/LOS binding rule (DECIDED by Instinct 10-17 10:04pm, not a quoted Luke ruling):
    keep the d124 source thresholds UNCHANGED (the 3.2 reach line, the reach>0.5 LOS rule);
    every combat signal is monitor-only/log-only - no heat, no cancel, no punish,
    punishEligible=false; any threshold change requires rig FP replay at 100/300 ms ping plus a
    reviewer GO. 0044 already embodies this (monitor-gated emit, thresholds untouched).
W3. FP-hardening promotion: FP-SPECS-COMBAT-HARDENING tunings (same-target consecutive-miss,
    per-tick dedup) and COMBAT-LAG-AUDIT fix1 are SOURCE-ONLY; each needs battery rows with
    100 ms and 300 ms ping passes (veil-checklist pattern) before any threshold ships.
W4. Battery family: nb-combat-cheat stops being CONDITIONAL once the hook+emit are live; new
    combat legit/cheat rows bind each enumerated path; every combat flag carries a
    CombatRecorder replay reference.
W5. Coverage/control-matrix/docs cleanup: the matrix's "computed, never emitted" wording was
    itself imprecise (no caller); coverage.md combat rows and the listing's combat watch line
    get corrected in the same pass, with monitor-log validation evidence for whatever the
    listing then claims.
W6. Reviewer rounds per step; nothing merges without a GO. CombatFeatures stays
    punishEligible=false until a separating signal is measured; it is not part of this re-arm.

Non-goals: no enforcement anywhere, ilhsmc untouched (nothing deploys there), no threshold
changes, no public claims until W5 lands.
