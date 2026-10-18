#!/usr/bin/env python3
"""Wiring guards for candidate6, not a native rig or performance test."""
import pathlib,re,unittest
S=(pathlib.Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Combat.java').read_text()
class Wiring(unittest.TestCase):
 def test_current_reach_not_erased(self):
  self.assertIn('double best = reach;',S);self.assertNotIn('best = Double.MAX_VALUE',S)
  self.assertIn('CombatEvidence.closest(best,',S)
 def test_aim_uses_coherent_old_eye_and_box(self):
  self.assertIn('AABB oldBox = sm.box.inflate(0.6);',S)
  self.assertIn('oldBox.clip(sm.eye, sm.eye.add(look.scale(6)))',S)
  self.assertIn('CombatEvidence.eitherAim(aimAccepted, oldAim)',S)
 def test_aimed_edge_hit_not_counted_as_multitarget_miss(self):
  self.assertIn('s.re[k] = aimAccepted ? 0.0 : 9.0;',S)
 def test_los_visibility_captured_at_sample_time(self):
  self.assertIn('AABB box, LosSnapshotCache.Result visibility)',S)
  self.assertIn('new Sample(tick, pl.level().dimension(), e.getUUID(), eye, e.getBoundingBox(), los)',S)
  self.assertIn('if (losSeen == 0 && losBlocked >= 2) hit = "hit through a block";',S)
  self.assertIn('if (sm.visibility == LosSnapshotCache.Result.VISIBLE) losSeen++;',S)
  self.assertNotIn('if (visible(pl, target, sm.eye, sm.box))',S)
 def test_ambiguous_flags_never_enter_verdict(self):
  method=S.split('private static void lowConfidence(',1)[1].split('\n\t}',1)[0]
  self.assertNotIn('Verdict.',method);self.assertIn('punishEligible=false',method)
  for pattern in ['attack rate','hitting before the cooldown','two targets in one tick','snapping between targets']:
   self.assertIn('lowConfidence(pl, "'+pattern+'")',S)
   self.assertNotIn('hit = "'+pattern+'"',S)
  self.assertIn('lowConfidence(pl, String.format("short-fall attack pattern',S)
  self.assertIn('lowConfidence(pl, "swap, hit and swap back',S)
  self.assertNotIn('Verdict.signal',S)
 def test_snap_requires_consecutive_qualifiers(self):
  self.assertIn('s.snap = CombatEvidence.nextConsecutive(s.snap,',S)
 def test_disconnect_clears_all_three_maps(self):
  self.assertIn('STATE.remove(id); TRACK.remove(id); MULTI.remove(id);',S)
 def test_geometry_positive_paths_preserved(self):
  for text in ['if (best > 3.2) hit =','hit = "hit through a block"','hit = "not looking at target"','return hit;']:
   self.assertIn(text,S)
if __name__=='__main__':unittest.main(verbosity=2)
