#!/usr/bin/env python3
"""Historical candidate5 predicate shapes only. Not candidate6 enforcement outcomes.
Source-coupled predicate simulations, not a Minecraft replay. Update with the rules."""
import pathlib, unittest
SOURCE=(pathlib.Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Combat.java').read_text()

class Candidate5Combat(unittest.TestCase):
    def anchor(self, fragment): self.assertIn(fragment,SOURCE,'candidate5 rule changed: review this test')

    # --- legal inputs must NOT convict (candidate4 audit reproducers, now negative) ---
    def test_legal_fast_clicks_under_rate_threshold(self):
        self.anchor('s.fast >= 12')
        fast=0
        for gap in [50]*4: fast=fast+1 if gap<70 else (0 if gap>=150 else max(0,fast-1))
        self.assertLess(fast,12)  # four 20CPS legal clicks do not convict
    def test_lag_bunched_clicks_under_rate_threshold(self):
        self.anchor('now - s.lastMs >= 150')
        fast=0
        for a,b in zip([1000,1001,1002,1003,1004],[1001,1002,1003,1004,1005]):
            fast=fast+1 if b-a<70 else (0 if b-a>=150 else max(0,fast-1))
        self.assertLess(fast,12)  # stalled-link release of 5 legal swings does not convict
    def test_legal_weak_spam_under_cooldown_threshold(self):
        self.anchor('s.weak >= 10')
        weak=0
        for charge,gap in [(.25,600)]*6:
            weak=weak+1 if (charge<.5 and gap<300) else max(0,weak-1)
        self.assertEqual(weak,0)  # slow weak swings never count at all
    def test_legal_fast_flicks_under_snap_threshold(self):
        self.anchor('turn > 60 && err < 0.6')
        self.anchor('s.snap >= 3')
        snap=0
        for changed,gap,turn,err in [(True,250,90,1.2),(True,250,90,1.5)]:
            if changed and gap<250 and turn>60 and err<0.6: snap+=1
            elif gap>600: snap=0
        self.assertEqual(snap,0)  # human 90deg flicks scatter (err>0.6) and never count
    def test_legal_batched_targets_under_multi_threshold(self):
        self.anchor('if (++mt[1] >= 4)')
        handled=[(1200,1),(1201,2),(1202,1)]
        multi=sum(t1-t0<50 and i0!=i1 for (t0,i0),(t1,i1) in zip(handled,handled[1:]))
        self.assertLess(multi,4)  # 3 legal clicks coalesced into one tick: 2 transitions, no verdict
    def test_real_descents_do_not_count_as_fake_crit(self):
        self.anchor('flatSinceLast')
        self.anchor('s.fakeCrit >= 10')
        fake=0; lastY=64.0
        for fd,dy in [(.125,-1.4)]*8:  # hopping down an edge: Y moves between attacks
            flat=abs(dy)<0.05
            if 0<fd<.3 and flat: fake+=1
            lastY+=dy
        self.assertEqual(fake,0)

    # --- attack shapes must STILL convict ---
    def test_historical_shape_sustained_fast_rate_convicts(self):
        fast=0
        for gap in [50]*12: fast=fast+1 if gap<70 else (0 if gap>=150 else max(0,fast-1))
        self.assertGreaterEqual(fast,12)
    def test_historical_shape_fast_weak_rhythm_convicts(self):
        weak=0
        for charge,gap in [(.25,120)]*10:
            weak=weak+1 if (charge<.5 and gap<300) else max(0,weak-1)
        self.assertGreaterEqual(weak,10)
    def test_historical_shape_precise_flicks_convict(self):
        snap=0
        for changed,gap,turn,err in [(True,200,95,0.3)]*3:
            if changed and gap<250 and turn>60 and err<0.6: snap+=1
            elif gap>600: snap=0
        self.assertGreaterEqual(snap,3)
    def test_historical_shape_multi_target_batch_convicts(self):
        handled=[(1200,1),(1201,2),(1202,3),(1203,4),(1204,5)]
        multi=sum(t1-t0<50 and i0!=i1 for (t0,i0),(t1,i1) in zip(handled,handled[1:]))
        self.assertGreaterEqual(multi,4)
    def test_historical_flat_endpoint_shape(self):
        fake=0; lastY=64.0
        for fd,dy in [(.08,0.0)]*10:  # spoofed micro-falls, Y never moves
            flat=abs(dy)<0.05
            if 0<fd<.3 and flat: fake+=1
            lastY+=dy
        self.assertGreaterEqual(fake,10)

    # --- metronome stays disabled (audit finding 6) ---
    def test_no_cadence_conviction_restored(self):
        self.anchor('if (Math.sqrt(var / 6) < 15) s.n = 0;')
        self.anchor('Same-target steadiness is not evidence of automation.')

if __name__=='__main__':unittest.main(verbosity=2)
