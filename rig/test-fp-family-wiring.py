#!/usr/bin/env python3
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac'
class Wiring(unittest.TestCase):
 def test_mining_rotation_live_hook(self):
  s=(R/'Movement.java').read_text();self.assertIn('Interact.look(pl, yaw, pitch); Mining.look(pl, yaw, pitch);',s)
  self.assertIn('CombatEvidence.recentTick(Ticks.n(pl), sd.aimCt, 10)',(R/'Mining.java').read_text())
 def test_interact_coherent_pose_live(self):
  s=(R/'Interact.java').read_text()
  for fragment in ['s.poses.add(','poses.valid(i,tnow,pl.level().dimension())','Vec3 oldEye=new Vec3(poses.x(i),poses.y(i),poses.z(i))','box.clip(oldEye,new Vec3(poses.endX(i,8)']:self.assertIn(fragment,s)
  self.assertIn('clicked a block through another block',s)
  look=s.split('public static void look(',1)[1].split('/** A bow',1)[0]
  self.assertNotIn('new InteractionPose',look);self.assertNotIn('new Vec3',look);self.assertNotIn('getEyePosition()',look)
 def test_step_shared_band_live(self):
  s=(R/'Movement.java').read_text()
  self.assertIn('StepEvidence.autoStep(ground, dy',s);self.assertIn('StepEvidence.excessive(ground, dy',s)
  self.assertIn('if (!StepEvidence.autoStep(ground, dy',s)
  self.assertIn('s.stepAcc.packet(ground, dy',s)
  acc=(R/'StepAccumulator.java').read_text();self.assertIn('LIMIT = 0.65',acc)
if __name__=='__main__':unittest.main(verbosity=2)
