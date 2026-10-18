#!/usr/bin/env python3
from pathlib import Path
import re,unittest
S=(Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Movement.java').read_text()
class Wiring(unittest.TestCase):
 def test_every_real_zero_reset_clears_shadow(self):
  matches=list(re.finditer(r's\.spBal = 0;',S));self.assertEqual(len(matches),6)
  for m in matches:self.assertTrue(S[m.end():].startswith(' s.spShadow = 0;'))
 def test_shadow_half_parity(self):
  self.assertIn('s.spShadow = SustainBudget.onExempt(s.spShadow, s.wasExempt)',S)
 def test_tick_budget_not_wall(self):
  body=S.split('private static void bedrockShadow',1)[1].split('\n\t}\n',1)[0]
  self.assertIn('getTickCount()',body);self.assertIn('BEDROCK_COMPARE_LOGS.take',body)
  self.assertNotIn('spShadowLogAt',S);self.assertNotIn('System.currentTimeMillis',body)
  for v in ['suppressedCaller','suppressedGlobal']:self.assertIn(v,body)
 def test_no_verdict_path(self):
  body=S.split('private static void bedrockShadow',1)[1].split('\n\t}\n',1)[0]
  for bad in ['Verdict.','hit =','spBal =']:self.assertNotIn(bad,body)
if __name__=='__main__':unittest.main(verbosity=2)
