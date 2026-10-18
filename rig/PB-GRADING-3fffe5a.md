# PB grading expectations, jar 3fffe5a (production-identical 9974eba)

Baselines from the bounce-7e live run (7e jar, legit rows PB-f..k already executed, clean),
raw reps from bounce-7e-result-9faea96f.json:
- slime-25 trampoline chain, 3 reps: peak_dy 1.256 / 1.361 / 1.579, zero SIGNAL. The rep
  spread is landing-tick alignment; grade against the rep envelope, not one rep. Note:
  1.579 is the same launch dy that flagged "jump dy 1.579" on candidate8 before the
  BounceTracker arm-ordering fix; its cleanliness at 7e is the arm-timing proof.
- slime-10 chain: zero SIGNAL (candidate8 artifacts "jump dy 0.862"/"2.881" were setback
  artifacts of the pre-fix jar, not vanilla trajectories).
- bed-10 chain, 2 reps: peak_dy 0.601 / 0.747, zero SIGNAL. (An earlier relay cited 0.787;
  the raw reps show 0.747 max - the windows below follow the raw.)
- honey-10 landing: no positive dy samples recorded, end y 70.9375 (the honey surface,
  sunk 1/16), zero SIGNAL.
- sneak-midchain-slime and sneak-slime-10: zero SIGNAL (sneak rows confirmed live).
- slime-to-honey cross: zero SIGNAL; end_pos y=52.2 is ~19 below pad level - the player
  left the pad structure after the cross-landing. No FP, but the fixture drifts; the rep
  should confirm the trajectory or wall the pad for re-runs.
- flat-jump control x3: peak_y 71.0 (= standing level), zero SIGNAL. If the jump legs had
  executed, peak_y would be ~72.25; treat the control as UNVERIFIED (jump likely never
  fired) and re-run it before grading any re-run of the legit rows.

Grading for 3fffe5a against those baselines. maxDy is graded offline from the captured
server log/fixture, not by pb-controls.py (the driver records evidence and SIGNAL rows only).

| Row | Signal expectation | Numeric expectation |
| --- | --- | --- |
| PB-f slime-25 (if re-run) | zero SIGNAL | peak_dy within [1.19, 1.66] (7e rep envelope 1.256..1.579, +-5%) |
| PB-g bed-10 (if re-run) | zero SIGNAL | peak_dy within [0.57, 0.78] (7e rep envelope 0.601..0.747, +-5%) |
| PB-j honey-10 (if re-run) | zero SIGNAL | no positive post-landing dy (any, silent, is a bounce-cover bug); end y ~70.9375 |
| PB-a..d powder legit | zero SIGNAL | climb rows ~0.2/tick; exit row 0.125 |
| PB-e / PB-l cheat controls | >=1 air jump/step SIGNAL | zero signals is INVALID, never PASS |
| PB-m rim legit | zero SIGNAL | jump dy 0.42 normal |
| PB-n rim step-cheat (near-not-in negative) | step SIGNAL | silence falsifies the body-intersection gate |
| PB-o slime edge | zero SIGNAL | record observed maxDy; partial bounce legal either way, flagging is not |
| PB-p bed-on-slime | zero SIGNAL | peak_dy within [0.57, 0.78] (bed envelope); >0.85 silent approaches slime-10 launch scale (~0.86) = over-cover hole (tracker read slime under the bed) |
| PB-q powder step-cheat (window-taint negative) | step SIGNAL | silence falsifies the 3fffe5a taint fix live |

A row PASSES only at its own expectation: legit rows zero-signal vs disabled baseline,
cheat rows on a real SIGNAL. No zero-signal shortcut to PASS on cheat rows.
