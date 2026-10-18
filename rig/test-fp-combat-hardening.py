#!/usr/bin/env python3
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Combat.java').read_text()
class Hardening(unittest.TestCase):
 def test_aim_window_in_live_path(self):
  self.assertIn('s.aimMisses.observe(target.getUUID(), target.getId(), pl.level().getServer().getTickCount()',S)
  self.assertIn('aimAccepted || reach <= 0.15 || hit != null',S)
 def test_multitarget_requires_distinct_ticks(self):self.assertIn('observedTicks.size() >= 4',S)
 def test_all_history_paths_validate_dimension_and_age(self):
  self.assertIn('pl.level().dimension().equals(sample.dimension)',S)
  self.assertIn('CombatEvidence.recentTick(tick, sample.tick, REWIND_TICKS)',S)
  self.assertIn('for (Sample sm : dq) if (validSample(pl, sm, target)) best',S)
  self.assertIn('if (!validSample(pl, sm, target)) continue; if (sm.visibility',S)
  self.assertIn('if (!validSample(pl, sm, target)) continue;\n\t\t\t\tAABB oldBox',S)
 def test_stable_subject_binding(self):
  self.assertIn('UUID identity',S)
  self.assertIn('target.getUUID(), sample.identity',S)
  self.assertIn('java.util.Objects.equals(old.dimension, dim)',S)
 def test_no_blanket_reach_widen_or_check_removal(self):
  for fragment in ['if (best > 3.2)','hit = "hit through a block"','hit = "not looking at target"','losBlocked >= 2']:
   self.assertIn(fragment,S)
 def test_ambiguous_patterns_stay_logged_not_promoted(self):
  for pattern in ['attack rate','hitting before the cooldown','two targets in one tick','snapping between targets']:
   self.assertIn('lowConfidence(pl, "'+pattern+'")',S)
   self.assertNotIn('hit = "'+pattern+'"',S)
if __name__=='__main__':unittest.main(verbosity=2)
