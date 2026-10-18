#!/usr/bin/env python3
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
class LogOnly(unittest.TestCase):
 def test_every_version_attacks_honor_returned_verdict(self):
  for source,handler in [('attack1211','handleInteract'),('attack26','handleAttack')]:
   with self.subTest(source=source):
    s=(ROOT/f'src/{source}/java/dev/meshac/mixin/AttackMixin.java').read_text()
    attack=s.split('private void meshac$attack',1)[1].split('\n\t}',1)[0]
    self.assertIn(f'method = "{handler}"',s)
    self.assertIn('Combat.check(player, e)',attack)
    self.assertIn('Verdict.Step step = Verdict.signal(player, "combat", hit, 2);',attack)
    self.assertIn('if (step != Verdict.Step.LOG) ci.cancel();',attack)
    self.assertEqual(attack.count('ci.cancel()'),1)
    self.assertLess(attack.index('Verdict.Step step ='),attack.index('if (step != Verdict.Step.LOG)'))
 def test_other_raw_denials_are_gated(self):
  for name,check in [('Interact','interact'),('Mining','mining')]:
   s=(ROOT/f'src/main/java/dev/meshac/{name}.java').read_text()
   self.assertIn(f'Config.get().enforce("{check}")',s)
   self.assertNotIn('return true;',s if name=='Interact' else s.split('public static boolean refuse',1)[1])
  for name in ['Vehicle','Totem']:
   s=(ROOT/f'src/main/java/dev/meshac/{name}.java').read_text()
   self.assertIn('step == Verdict.Step.LOG',s)
   self.assertIn('ActionEvidence.cancel(Config.get().enforce(',s)
 def test_feature_switches_not_monitor_switches(self):
  cfg=(ROOT/'src/main/java/dev/meshac/Config.java').read_text()
  self.assertIn('return enderchestEggRule == null || enderchestEggRule;',cfg)
  self.assertIn('return veilEnabled == null || veilEnabled;',cfg)
  self.assertIn('Veil.xrayOn = get().veilEnabled()',cfg)
  self.assertIn('Veil.espOn = get().veilEnabled()',cfg)
  for v in ['slot1211','slot26']:
   s=(ROOT/f'src/{v}/java/dev/meshac/mixin/SlotMixin.java').read_text()
   self.assertIn('Config.get().enderchestEggRule() && hasEgg',s)
  doc=(ROOT/'docs/veil/OPERATOR-GUIDE.md').read_text()
  self.assertIn('not governed by monitor mode',doc)
if __name__=='__main__': unittest.main()
