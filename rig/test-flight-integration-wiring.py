#!/usr/bin/env python3
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Movement.java').read_text()
class Wiring(unittest.TestCase):
 def test_pending_before_any_check(self):
  b=S.split('public static double[] check(',1)[1]
  i=b.index('if (s.pending)')
  for marker in ['if (p.hasRotation())','if (s.blinkHit)','s.fallPeak','return statusOnly']:self.assertLess(i,b.index(marker))
  guard=b[i:b.index('if (Config.get().exempt')]
  self.assertIn('if (p.hasPosition())',guard);self.assertEqual(guard.count('s.arrivals.poll()'),1)
  for marker in ['Verdict.','fallPeak','Interact.look','Mining.look']:self.assertNotIn(marker,guard)
 def test_union_preserved(self):
  for marker in ['FpGuards.stuckExpect','s.bounce.bouncing','s.stepAcc.packet','s.clock.arrive','CTX_LOGS.take(s.ctxBudget','CTX_LOGS.take(s.noclipBudget']:self.assertIn(marker,S)
if __name__=='__main__':unittest.main(verbosity=2)
