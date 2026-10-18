#!/usr/bin/env python3
from pathlib import Path
import importlib.util,unittest
ROOT=Path(__file__).resolve().parents[1]
P=ROOT/'rig/fp-combat-native-draft.py'
spec=importlib.util.spec_from_file_location('draft',P);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Draft(unittest.TestCase):
 def test_plans(self):
  self.assertEqual(m.plan('FP-1105',6)['durationSeconds'],60)
  self.assertEqual(m.plan('FP-1107',10)['holdMs'],[500,1000,2000])
 def test_scope_and_opt_in(self):
  s=P.read_text()
  for fragment in ["if not a.execute:","m['rig']!='meshac-red'","moduleOffPixelsReviewed","proxyRigBindingReviewed","stat.S_ISSOCK","baselineDamage"]:self.assertIn(fragment,s)
  for fragment in ["iptables","nft ","SIGKILL","SIGTERM","docker restart","docker stop"]:self.assertNotIn(fragment,s)
 def test_reducer_is_not_wired_to_damage(self):
  s=(ROOT/'src/main/java/dev/meshac/CombatReconciliation.java').read_text()
  for bad in ['Verdict.','ServerPlayer','teleport','hurt(','LOG.']:self.assertNotIn(bad,s)
  self.assertNotIn('Decision.ALLOW',s)
 def test_proof_not_automatic(self):
  self.assertIn("'proofStatus':'UNVERIFIED",P.read_text())
if __name__=='__main__':unittest.main(verbosity=2)
