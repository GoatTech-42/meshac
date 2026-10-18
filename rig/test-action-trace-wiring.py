#!/usr/bin/env python3
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac'
class Wiring(unittest.TestCase):
 def test_off_by_default(self):
  s=(R/'ActionTrace.java').read_text();self.assertIn('"1".equals(System.getenv("MESHAC_ACTION_TRACE"))',s)
  self.assertLess(s.index('if (!ON || !BUDGET.take'),s.index('fields.get()'))
  self.assertIn('catch (RuntimeException ignored)',s)
 def test_no_verdict_mutation_or_network(self):
  s=(R/'ActionTrace.java').read_text()
  for forbidden in ['Verdict.signal','connection.send','setBlockState','disconnect(','Files.','new Thread(']:self.assertNotIn(forbidden,s)
 def test_required_hooks(self):
  for file,markers in [('Mining.java',['"start"','"stop"','"break-attempt"']),('Totem.java',['"totem-pop"','"totem-click"']),('Vehicle.java',['"boat-move"']),('mixin/ServerGamePacketListenerImplMixin.java',['"container-click"','clickKind(p)'])]:
   s=(R/file).read_text()
   for marker in markers:self.assertIn(marker,s)
 def test_rate_uses_server_progress(self):
  s=(R/'Mining.java').read_text();self.assertIn('getDestroyProgress(pl, pl.level(), pos)',s)
  self.assertIn('s.breakWork += MiningRate.cost(progress)',s)
if __name__=='__main__':unittest.main(verbosity=2)
