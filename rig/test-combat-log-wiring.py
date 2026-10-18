#!/usr/bin/env python3
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Combat.java').read_text()
class Logging(unittest.TestCase):
 def test_each_combat_log_has_budget(self):
  for line in S.splitlines():
   if 'Meshac.LOG.info' in line:
    with self.subTest(line=line):self.assertIn('.logs.allow(',line)
 def test_swap_timeline_is_bounded(self):
  line=next(l for l in S.splitlines() if 'Trace.t(' in l)
  self.assertIn('Trace.ON && s.logs.allow(CombatLogBudget.Kind.TRACE',line)
 def test_raw_history_update_is_outside_emission_guard(self):
  self.assertIn('if (s.logs.allow(CombatLogBudget.Kind.TRACE, ns)) Meshac.LOG.info',S)
  self.assertIn('s.traceNs = ns; s.traceTick = tick;',S)
 def test_summary_and_evidence_are_distinct_caps(self):
  self.assertIn('CombatLogBudget.Kind.SUMMARY',S)
  self.assertIn('CombatLogBudget.Kind.EVIDENCE',S)
  self.assertIn('state.logs.takeSuppressed(CombatLogBudget.Kind.EVIDENCE)',S)
if __name__=='__main__':unittest.main(verbosity=2)
