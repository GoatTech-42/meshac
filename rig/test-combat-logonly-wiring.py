#!/usr/bin/env python3
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
class LogOnly(unittest.TestCase):
 def test_every_version_attacks_never_enforce(self):
  # 0055: combat is log-only BY CONSTRUCTION in every config - both attack handlers call
  # Verdict.signalLogOnly and can never cancel, whatever monitor mode says.
  for source,handler in [('attack1211','handleInteract'),('attack26','handleAttack')]:
   with self.subTest(source=source):
    s=(ROOT/f'src/{source}/java/dev/meshac/mixin/AttackMixin.java').read_text()
    attack=s.split('private void meshac$attack',1)[1].split('\n\t}',1)[0]
    self.assertIn(f'method = "{handler}"',s)
    self.assertIn('Combat.check(player, e)',attack)
    self.assertIn('Verdict.signalLogOnly(player, "combat", hit, 2);',attack)
    self.assertNotIn('Verdict.signal(player, "combat"',attack)
    self.assertNotIn('ci.cancel()',attack)
    attack_inj=[x for x in s.split('@Inject') if 'meshac$attack' in x]
    self.assertEqual(len(attack_inj),1)
    self.assertNotIn('cancellable = true',attack_inj[0])
 def test_signallogonly_never_enforces(self):
  import re
  v=(ROOT/'src/main/java/dev/meshac/Verdict.java').read_text()
  v=re.sub(r'/\*.*?\*/','',v,flags=re.S); v=re.sub(r'//[^\n]*','',v)
  self.assertIn('public static Step signalLogOnly(',v)
  body=v.split('public static Step signalLogOnly(',1)[1].split('\n\t}',1)[0]
  self.assertIn('h.mon.add(',body)   # real mon heat for the rig
  self.assertNotIn('h.lh.add(',body) # never feeds the removal ladder
  self.assertNotIn('remove(',body)
  self.assertNotIn('Step.REMOVE',body)
  self.assertIn('return Step.LOG;',body)
  self.assertIn('sigLog',body)       # log/alert budget (proposal pre-GO 1)
  self.assertIn('"combat".equals(check)',body)  # 0057: budget is combat-only, other families unthrottled
  self.assertIn('System.nanoTime()',body)       # 0057: monotonic clock, wall rollback cannot reopen
  self.assertIn('now - b[0] >= 60_000_000_000L',body)
  self.assertNotIn('ms < b[0]',body)            # no reopen-on-rollback branch
  self.assertIn('SUPPRESSED_FMT = "[meshac] SIGNAL-SUPPRESSED {} {} {} count={} ({})";',v)  # 0059: ONE receipt shape, name+uuid stable identity
  self.assertIn('String name;',v)               # 0059: H carries the player name
  fgt=v.split('public static void forget(',1)[1].split('\n\t}',1)[0]
  self.assertIn('flushSuppressed(id, h, "disconnect flush")',fgt)  # 0057/0059: disconnect flush via the shared receipt
  cpa=v.split('public static void clearPendingActions(',1)[1].split('\n\t}',1)[0]
  self.assertIn('flushSuppressed',cpa) and cpa.index('flushSuppressed')<cpa.index('HEAT.clear()')  # 0059: flush-before-clear (monitor toggle)
  self.assertIn('public static void tickFlush(',v)  # 0059: idle-connected pump
  tf=v.split('public static void tickFlush(',1)[1].split('\n\t}',1)[0]
  self.assertIn('System.nanoTime()',tf) and self.assertIn('flushSuppressed(pl.getUUID(), h, "window closed (idle, tick flush)")',tf)
  wc=v.split('public static Step signalLogOnly(',1)[1].split('\n\t}',1)[0]
  self.assertIn('flushSuppressed(pl.getUUID(), h, "window closed")',wc)  # signal-driven close uses the same receipt
  mg=(ROOT/'src/main/java/dev/meshac/Meshac.java').read_text()
  self.assertIn('Verdict.tickFlush(pl);',mg)   # 0059: wired into END_SERVER_TICK
  self.assertIn('if (cfg.monitor()) return signalLogOnly(pl, check, detail, weight);',v)  # one implementation
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
