#!/usr/bin/env python3
# 0064: static pins for rig/SERVER-THREAD-REENTRY-PROOF.md - every thread-evidence claim
# the paper makes is pinned against the source it cites. Comment-stripped reads.
from pathlib import Path
import re, unittest
ROOT = Path(__file__).resolve().parents[1]
def stripped(p):
    s = (ROOT/p).read_text()
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    return re.sub(r'//[^\n]*', '', s)
def body(s, sig):
    return s.split(sig, 1)[1].split('\n\t}', 1)[0]
class ReentryPins(unittest.TestCase):
    def test_attack_mixins_guarded(self):
        for tree in ['attack1211', 'attack26']:
            with self.subTest(tree=tree):
                s = stripped(Path(f'src/{tree}/java/dev/meshac/mixin/AttackMixin.java'))
                a = body(s, 'private void meshac$attack')
                self.assertIn('if (!player.level().getServer().isSameThread()) return;', a)
                self.assertLess(a.index('isSameThread()) return;'), a.index('Combat.check('))  # guard BEFORE any Verdict reach
    def test_two_pass_netty_branches_touch_no_verdict(self):
        m = stripped(Path('src/main/java/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java'))
        self.assertIn('if (!player.level().getServer().isSameThread()) { Movement.arrive(player, p); return; }', m)
        self.assertIn('if (!player.level().getServer().isSameThread()) { Inventory.arrive(player); return; }', m)
        mv = stripped(Path('src/main/java/dev/meshac/Movement.java'))
        self.assertNotIn('Verdict', body(mv, 'public static void arrive('))            # netty pass: own state only
        te_line = [l for l in mv.splitlines() if 'public static void tickEnd(' in l]
        self.assertEqual(len(te_line), 1); self.assertNotIn('Verdict', te_line[0])     # netty pass: own state only
        iv = stripped(Path('src/main/java/dev/meshac/Inventory.java'))
        arr_line = [l for l in iv.splitlines() if 'public static void arrive(' in l]
        self.assertEqual(len(arr_line), 1); self.assertNotIn('Verdict', arr_line[0])
        t = stripped(Path('src/main/java/dev/meshac/Totem.java'))
        pop = [l for l in t.splitlines() if 'public static void popped(' in l]
        self.assertEqual(len(pop), 1); self.assertNotIn('Verdict', pop[0])             # Totem.popped: no Verdict
        sg = body(stripped(Path('src/main/java/dev/meshac/Interact.java')), 'public static long signGap(')
        self.assertNotIn('Verdict.signal', sg)                                          # sign hook netty part: no Verdict
    def test_every_mixin_verdict_call_is_dispatched_or_guarded(self):
        m = stripped(Path('src/main/java/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java'))
        # sign + chat: the ONLY Verdict.signal calls in the mixin, both inside server.execute
        self.assertEqual(m.count('Verdict.signal('), m.count('.execute(() -> dev.meshac.Verdict.signal('))
        self.assertEqual(m.count('Verdict.signal('), 2)
        self.assertNotIn('.sign(', m)  # the mixin never calls the signaling Interact.sign variant
        # guarded single-pass branches (guard text present at the call site)
        for frag in ['isSameThread() && Vehicle.check(player, p)',
                     'isSameThread()) { Combat.swapped(player, p.getSlot()); Interact.swap(player); Inventory.hotbar(player);',
                     'START_FALL_FLYING && player.level().getServer().isSameThread()',
                     'SWAP_ITEM_WITH_OFFHAND && player.level().getServer().isSameThread()',
                     'isSameThread() && !player.isCreative()',
                     'if (!player.level().getServer().isSameThread()) Movement.tickEnd(player); else dev.meshac.Ticks.inc(player);']:
            self.assertIn(frag, m)
        for meth in ['meshac$place', 'meshac$use']:  # useItemOn / useItem: work only inside the isSameThread branch
            self.assertIn('if (player.level().getServer().isSameThread()) {', body(m, f'private void {meth}('))
    def test_destroyblock_guarded_since_0065(self):
        g = stripped(Path('src/main/java/dev/meshac/mixin/ServerPlayerGameModeMixin.java'))
        self.assertIn('Mining.refuse(player, pos)', g)
        b = g.split('private void meshac$break', 1)[1]
        self.assertIn('if (!player.level().getServer().isSameThread()) return;', b)
        self.assertLess(b.index('isSameThread()) return;'), b.index('Mining.refuse('))  # 0065: guard BEFORE world reads / Verdict reach
        self.assertNotIn('.execute(', b)  # 0064 reviewer: NO server.execute - a delayed refusal would change synchronous cancel semantics
        mg = stripped(Path('src/main/java/dev/meshac/Meshac.java'))
        tick = mg.split('END_SERVER_TICK.register', 1)[1].split('ServerPlayConnectionEvents', 1)[0]
        self.assertIn('Verdict.tickFlush(pl);', tick)
        disc = mg.split('DISCONNECT.register', 1)[1][:3000]  # one-line lambda
        self.assertIn('Verdict.forget(handler.player.getUUID());', disc)
        mc = stripped(Path('src/main/java/dev/meshac/MeshCommands.java'))
        mon = [l for l in mc.splitlines() if '"on"' in l or '"off"' in l]
        self.assertTrue(any('Verdict.clearPendingActions()' in l for l in mon))
        self.assertIn('Verdict.status(t)', mc)  # read-only route exists (Heat.now non-mutating, reviewer-confirmed)
        cf = stripped(Path('src/main/java/dev/meshac/Config.java'))
        self.assertIn('Verdict.clearPendingActions();', cf)  # reload route (server-thread brigadier dispatch)
    def test_lock_coverage_boundaries(self):
        v = stripped(Path('src/main/java/dev/meshac/Verdict.java'))
        slo = body(v, 'public static Step signalLogOnly(')
        self.assertLess(slo.index('h.name = pl.getGameProfile().name();'), slo.index('synchronized (h)'))  # name write OUTSIDE the lock (visibility via monitor-edge happens-before)
        self.assertEqual(slo.count('h.name = '), 1)
        fgt = body(v, 'public static void forget(')
        self.assertLess(fgt.index('HEAT.remove(id)'), fgt.index('flushSuppressed(id, h, "disconnect flush")'))  # remove-then-flush, documented
        self.assertIn('ConcurrentHashMap', v)  # HEAT lifecycle relies on CHM publication
        fs = body(v, 'private static void flushSuppressed(')
        self.assertIn('synchronized (h)', fs)
    def test_paper_states_the_boundaries(self):
        p = (ROOT/'rig/SERVER-THREAD-REENTRY-PROOF.md').read_text()
        for frag in ['destroyBlock', 'monitor-edge happens-before', 'ConcurrentHashMap',
                     'flush loop over HEAT.entrySet()', 'Heat.now returns the decayed value',
                     'thread-assertion', 'fail-safe direction', 'server.execute', 'detection LOSS',
                     'in-flight-reference', 'DETACHED', 'NOT an']:
            self.assertIn(frag, p)
if __name__ == '__main__':
    unittest.main()
