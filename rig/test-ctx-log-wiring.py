#!/usr/bin/env python3
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Movement.java').read_text()
class Wiring(unittest.TestCase):
 def test_budget_before_payload(self):
  s=S.split('private static void ctxLog(',1)[1].split('\n\t}',1)[0]
  self.assertLess(s.index('CTX_LOGS.take(s.ctxBudget, System.nanoTime())'),s.index('pl.getLastDamageSource()'))
  self.assertLess(s.index('CTX_LOGS.take'),s.index('MoveContext.line('))
  for text in ['droppedCaller','droppedGlobal']:self.assertIn(text,s)
  self.assertNotIn('Verdict.',s)
if __name__=='__main__':unittest.main(verbosity=2)
