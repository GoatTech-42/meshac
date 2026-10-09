# final sweep (beta candidate)

jar: dist meshac-26.2 (ladder fix 3729202). 25 meteor sweeps, 138 cases, 57 legit controls: 0 signals on controls, no false positives.

## kill-aura is intermittent
kill-aura signals come from attack-rate and before-cooldown counters. a patient, aimed kill-aura is not caught every run (one rerun gave 6 signals, the next 0). this is a coverage limit, not a false positive.

## known misses
bow aimbot, explosion velocity, blink under 0.4 s, boost over 45 b/s.

## chunkveil (off by default: veilXray / veilEsp null)
- anti-xray: protocol bot sees 83/83 ores with it off. on: 0/80 buried ores, 3/3 exposed ores, digging reveals neighbours. about 2% of stone is fake ore while it is on.
- esp raycast: victim behind a wall hidden 0/50 samples; open, near and glass visible 100%; teleport into view shows in 205 ms. leaves hide the victim too (fancy-graphics see-through is not matched).
- freecam: client-side only, the server sends entities by the real player position, so the wall test covers it.
- legit pass: exposed ores identical on vs off. no long scripted mining run, no real-client run.
- corner-peek with a walking victim: inconclusive, victim did not walk in the test.
