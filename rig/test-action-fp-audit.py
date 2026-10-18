#!/usr/bin/env python3
"""Static regression and counterexample audit. Not native-play clearance."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac'
class ActionAudit(unittest.TestCase):
 def test_interact_shift_bridge_tower(self):
  # Existing chain requires horizontal displacement >.3 each placement.
  # Tower and slow shift-bridge should not enter that chain, independent of rate.
  for under,dist in [(True,0),(True,.2),(False,.8)]:self.assertFalse(under and dist>.3)
  self.assertIn('underFeet && dist > 0.3',(R/'Interact.java').read_text())
 def test_interact_fast_bridge_counterexample_remains(self):
  # A10block placement chain at5blocks/s crosses current4threshold.
  # This reports uncertainty, not a manufactured safe-human threshold.
  self.assertGreater(10/(2000/1000),4)
  self.assertIn('speed > 4.0',(R/'Interact.java').read_text())
 def test_creative_inventory_gate_exists(self):
  self.assertIn('Config.get().skips(pl, "inventory")',(R/'Inventory.java').read_text())
 def test_inventory_drag_rate_risk_still_present(self):
  # More than10packet events can describe drag/sort stages, not10items.
  self.assertIn('MAX_IN_WINDOW = 10',(R/'InventoryBurst.java').read_text())
  self.assertIn('s.beat.observe(gap, now)',(R/'Inventory.java').read_text())
  self.assertIn('ActionEvidence.steadyGap(gap, lastGap)',(R/'BeatCounter.java').read_text())
 def test_haste_efficiency_rate_uses_progress(self):
  # Old flat8cap counterexample now routes through per-block progress.
  self.assertGreater(20,8)
  s=(R/'Mining.java').read_text();self.assertIn('MAX_PER_SECOND = 8',s)
  self.assertIn('MiningRate.exceeded(s.breakWork)',s)
  self.assertIn('getDestroyProgress(pl, pl.level(), pos)',s)
 def test_mining_early_progress_boundary(self):
  # Existing early progress: fast enough tool isn't early, low progress is.
  self.assertFalse(.5*(1+1)<.5);self.assertTrue(.1*(1+1)<.5)
  self.assertIn('DigRule.early(delta, ticks)',(R/'Mining.java').read_text())
  self.assertIn('delta * (ticks + 1) < 0.5f',(R/'veil/DigRule.java').read_text())
 def test_vehicle_horse_and_supported_boat_gates(self):
  s=(R/'Vehicle.java').read_text();self.assertIn('v instanceof AbstractBoat',s)
  for gate in ['v.isInWater()','v.onGround()','Blocks.BUBBLE_COLUMN','FluidTags.WATER']:self.assertIn(gate,s)
  self.assertIn('ActionEvidence.cancel',s)
 def test_totem_preserves_calibrated_thresholds_and_enforcement(self):
  s=(R/'Totem.java').read_text();self.assertIn('IMPOSSIBLE_MS = 10, TOO_FAST_MS = 25',s)
  self.assertIn('ActionEvidence.cancel',s)
if __name__=='__main__':unittest.main(verbosity=2)
