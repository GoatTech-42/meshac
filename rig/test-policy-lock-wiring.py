#!/usr/bin/env python3
"""0091 policy-transition atomicity, static wiring proof:
- policy + epoch publish as ONE immutable Config.Snapshot, read once per decision (no torn independent volatiles);
- a policy COMMIT (reload; monitor-on toggle) and every decision AND its effects are mutually exclusive via
  Config.POLICY_LOCK, so no action computed under one policy can dispatch after another has committed and the
  commit's HEAT wipe cannot interleave with a decision's HEAT access.
Concurrency itself is NOT proven here: the claim rests on Java monitor semantics over this wiring. The threaded
signal/reload stress test is MC/harness-side work owed by Box (no pure unit test: Verdict needs ServerPlayer)."""
from pathlib import Path
import re, unittest
ROOT = Path(__file__).resolve().parents[1]
S = ROOT/'src/main/java/dev/meshac'
def strip(t):
 t = re.sub(r'/\*.*?\*/','',t,flags=re.S); return re.sub(r'//[^\n]*','',t)
def body(text, needle):
 a=text.index(needle); b=text.index('{',a); depth=1; i=b+1
 while depth:
  depth += (text[i]=='{') - (text[i]=='}'); i += 1
 return text[a:i]
class PolicyLockWiring(unittest.TestCase):
 def test_snapshot_bundle(self):
  c = strip((S/'Config.java').read_text())
  self.assertIn('public static final class Snapshot', c)
  snap = body(c, 'public static final class Snapshot')
  self.assertIn('public final Config cfg;', snap); self.assertIn('public final long epoch;', snap)
  self.assertIn('public static Snapshot current()', c)
  self.assertIn('public static final Object POLICY_LOCK', c)
  self.assertNotIn('reloadEpoch', c)  # the 0090 independent-volatile token could tear; gone
 def test_reload_commit_atomic(self):
  c = strip((S/'Config.java').read_text())
  load = body(c, 'public static AdminLog.ReloadResult load(')
  self.assertIn('synchronized (POLICY_LOCK)', load)
  lock = load[load.index('synchronized (POLICY_LOCK)'):]
  self.assertLess(lock.index('Verdict.clearPendingActions()'), lock.index('cur = new Snapshot('))  # wipe BEFORE publish, inside the lock
 def test_decisions_hold_the_lock(self):
  v = strip((S/'Verdict.java').read_text())
  self.assertNotIn('reloadEpoch', v)  # the 0090 check-then-act guard is superseded by mutual exclusion
  sig = body(v, 'public static Step signal(ServerPlayer pl, String check, String detail, int weight)')
  self.assertIn('synchronized (Config.POLICY_LOCK)', sig)
  locked = body(v, 'private static Step signalLocked(')
  self.assertIn('Config.current().cfg', locked)   # snapshot read once
  self.assertNotIn('Config.get()', locked)        # no second, potentially torn read inside the decision
  self.assertIn('remove(pl, why, ev, cfg)', locked)  # the effect receives the decision's own snapshot
  for needle in ['public static Step signalLogOnly(', 'public static void note(']:
   self.assertIn('synchronized (Config.POLICY_LOCK)', body(v, needle), needle)
  rem = body(v, 'private static void remove(')
  self.assertIn('Config cf)', rem.split('{',1)[0])
  self.assertNotIn('Config.get()', rem)
  self.assertIn('cf.enforce(', rem)
 def test_monitor_toggle_is_a_locked_commit(self):
  mc = strip((S/'MeshCommands.java').read_text())
  on = [l for l in mc.splitlines() if '"on"' in l and 'monitorOnly = true' in l]
  self.assertEqual(len(on), 1)
  self.assertIn('synchronized (Config.POLICY_LOCK)', on[0])
  self.assertLess(on[0].index('synchronized (Config.POLICY_LOCK)'), on[0].index('Verdict.clearPendingActions()'))
  off = [l for l in mc.splitlines() if '"off"' in l and 'monitorOnly = false' in l]
  self.assertEqual(len(off), 1)
  self.assertIn('synchronized (Config.POLICY_LOCK)', off[0])
 def test_load_uses_snapshot_types(self):
  # 0091 shipped two compile errors my regex tests could not see (cur used as Config). Guard the exact shapes.
  c = strip((S/'Config.java').read_text())
  load = body(c, 'public static AdminLog.ReloadResult load(')
  self.assertNotIn('= cur;', load)           # cur is a Snapshot, never a Config
  self.assertNotIn('(g, cur)', load)
  # 0093/0094: missing-file load validates a round-trip COPY, never the published instance, captured under the lock
  self.assertIn('next = g.fromJson(g.toJson(cur.cfg), Config.class);', load)
  self.assertIn('documented(g, next)', load)
  i_lock = load.index('synchronized (POLICY_LOCK)')
  i_copy = load.index('next = g.fromJson(g.toJson(cur.cfg), Config.class);')
  self.assertLess(i_lock, i_copy)  # 0094: copy captured under the commit lock - no toggle can interleave
  # 0093: validation is inside the commit lock, before the wipe and the publish
  i_val = load.index('next.validatePolicy()')
  i_wipe = load.index('Verdict.clearPendingActions()')
  i_pub = load.index('cur = new Snapshot(')
  self.assertLess(i_lock, i_val); self.assertLess(i_val, i_wipe); self.assertLess(i_wipe, i_pub)
 def test_disabledchecks_volatile_read_once(self):
  c = strip((S/'Config.java').read_text())
  self.assertIn('public volatile java.util.List<String> disabledChecks', c)
  dis = body(c, 'public boolean disabled(')
  self.assertIn('var l = disabledChecks;', dis)   # read the volatile reference once
  self.assertEqual(dis.count('disabledChecks'), 1)
 def test_setcheck_copy_on_write_locked(self):
  mc = strip((S/'MeshCommands.java').read_text())
  sc = body(mc, 'private static int setCheck(')
  self.assertIn('synchronized (Config.POLICY_LOCK)', sc)
  self.assertIn('new java.util.ArrayList<String>()', sc)  # copy-on-write: never mutate the published list in place
  self.assertIn('Config.get().disabledChecks = next;', sc)
  lock = sc[sc.index('synchronized (Config.POLICY_LOCK)'):sc.index('Config.get().disabledChecks = next;')]
  self.assertNotIn('l.removeIf', lock)  # no in-place mutation of the published list inside the lock
 def test_every_policylock_reference_is_an_acquisition(self):
  for f in ['Verdict.java','Config.java','MeshCommands.java']:
   t = strip((S/f).read_text())
   refs = t.count('POLICY_LOCK')
   decl = t.count('Object POLICY_LOCK = new Object()')
   self.assertEqual(refs - decl, t.count('synchronized (Config.POLICY_LOCK)') + t.count('synchronized (POLICY_LOCK)'), f)
if __name__ == '__main__': unittest.main()
