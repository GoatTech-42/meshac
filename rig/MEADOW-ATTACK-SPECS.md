# Meadow attack-module specs (detection-side contracts, for the meadow lane)

Two specs so the blocked control rows unblock. Both are written from meshac d124 source; the
meadow lane owns implementation style, these own the OBSERVABLE contract (what the packets must
look like) so the control rows prove detection, not setup luck.

## 1. MicroHop module (unblocks nb-microhop-cheat)

The check (Movement.java:298-306): when the server last knew the player grounded, a packet with
0.005 < dy < 0.30 whose DESTINATION has no collision 0.03 below, no ceiling within +0.6 above the
head, not near powder, not bouncing/in water/climbing/riding/flying counts bufHop += 2.
bufHop >= 3 fires "microhop dy %.3f"; hopClock forgives one hop per ~1.5 s (bufHop -1 every 30
ticks). So: TWO qualifying hops inside ~1.5 s flag.

Observable contract:
- From a grounded state on a flat open pad, emit a small hop with dy in [0.10, 0.25] (safely
  inside the band; 0.42 is a real jump and goes to the jump check instead), twice within ~1 s.
- Implementation sketch: while enabled, moving, and on ground, every 15-20 ticks set the player's
  deltaMovement.y to 0.2 once; the normal position packets carry it. No packet forgery needed -
  the server recomputes support at the destination.
- Guards the module must respect so the row stays a CLEAN control (not an FP argument): flat pad,
  no ceiling within 3 blocks, no powder nearby, no bounce blocks. Those guards exist for Luke's
  2026-10-11 sprint-jump-under-ceiling and powder-struggle FPs; the legit twin (nb-microhop-legit,
  snow ramp + 2-block ceiling) exercises exactly them.
- Note: packet-criticals and MaceDMG produce this same pattern on every hit (source comment) - the
  module is the movement-only member of that family.

## 2. NoFall interleave mode (unblocks GS-C live)

Round 2's evasion (GS01) was fake +gnd claims interleaved with honest air packets; the old
3-consecutive buffer reset on any honest packet. Patch 1's window changes that: ONLY GROUND CLAIMS
enter the window - honest air packets neither add nor evict (Movement.java:320-336, rebind delta 1).
Dilution needs REAL supported ground contact, not air.

Observable contract:
- New NoFall setting (e.g. interleave = k): during a fall past the distance threshold, send a fake
  StatusOnly(onGround=true) every k-th airborne packet, honest air packets between.
- Pre-registered outcome (REDTEAM-GROUNDSPOOF-PLAN GS-C): CAUGHT for every k - the fake claims all
  enter the window and 3-of-5 accumulates regardless of the air between them (air never dilutes).
  If a run shows silence with attack-effect proof, that is a MEADOW-EVADE finding against the window.
- The follow-on evasion that WOULD stress the window is touch-and-go micro-landings (brief REAL
  supported contact draining evidence mid-fall) - preregistered as GS-C2 (2-of-5 density,
  BY-DESIGN below-threshold). It needs a landing-survival assist (the MicroHop spec in this file);
  out of scope for NoFall alone. Never confuse air-interleave (GS-C, caught) with
  landing-interleave (GS-C2, hole).

## Fixture status (GS-C replay assets)

- My bundle holds only SYNTHETIC grader fixtures for GS01-GS03 (verdict-path pins in
  rig/test-pb-grade.py, patch 0011) - they grade expected verdicts, they cannot drive a client.
- The round-2 interleaved run was the FP lane's rig observation; its logs live in the FP lane's
  repo (~/fp-checklist on Box) - no packet-level recording exists in my bundle.
- Offline, the window's behavior against any captured gndclaim/gndclear stream is already scorable
  by sim/GroundReplay (both clear policies) without a live client.
