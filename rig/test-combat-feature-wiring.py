#!/usr/bin/env python3
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
S=(ROOT/'src/main/java/dev/meshac/Combat.java').read_text()
F=(ROOT/'src/main/java/dev/meshac/CombatFeatures.java').read_text()
class Wiring(unittest.TestCase):
 def test_no_feature_verdict_or_score(self):
  self.assertNotIn('Verdict.',F)
  block=S.split('// Features 1/7/10',1)[1].split('if (pl.getMainHandItem()',1)[0]
  self.assertNotIn('hit =',block);self.assertNotIn('Verdict.',block)
  self.assertEqual(block.count('punishEligible=false'),2)
 def test_raw_trace_gated(self):self.assertIn('if (Trace.ON && s.logs.allow(CombatLogBudget.Kind.TRACE, System.nanoTime())) Meshac.LOG.info("[combat-features-raw]',S)
 def test_no_small_window_summary(self):self.assertIn('evidence.n() >= 8',S)
if __name__=='__main__':unittest.main(verbosity=2)
