#!/usr/bin/env python3
"""The LOS verdict must count observed samples: UNKNOWN must not veto, and a
single blocked observation is not enough (a target entering tracking late)."""
import pathlib, unittest
SRC = (pathlib.Path(__file__).resolve().parents[1] / 'src/main/java/dev/meshac/Combat.java').read_text()
class LosVerdict(unittest.TestCase):
    def test_unknown_does_not_veto(self):
        self.assertIn('if (losSeen == 0 && losBlocked >= 2) hit = "hit through a block";', SRC)
        self.assertNotIn('sm.visibility != LosSnapshotCache.Result.BLOCKED', SRC)
    def test_visible_vetoes(self):
        self.assertIn('if (sm.visibility == LosSnapshotCache.Result.VISIBLE) losSeen++;', SRC)
    def test_trace_full_bypass_is_trace_only(self):
        budget = (pathlib.Path(__file__).resolve().parents[1] / 'src/main/java/dev/meshac/CombatLogBudget.java').read_text()
        self.assertIn('if (i == 2 && FULL_TRACE) return true;', budget)
if __name__ == '__main__':
    unittest.main()
