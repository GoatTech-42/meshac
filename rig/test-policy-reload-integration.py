#!/usr/bin/env python3
"""0097: PARTIAL integration fixture for the reviewer-owed MC-side integration tests (0086), reviewer round on 0096 folded.
Real production classes: Config, Ladder, Verdict, Cases, SafeSwap, AdminLog, CheckNames, ActionEvidence, Vehicle -
compiled with a REAL Gson jar (the reload path is half Gson; the monitor-invariant doubles fake it). Minecraft runtime
is service doubles. Scope labels, stated honestly:
- S2 uses the REAL direct-cancel caller (Vehicle.check -> Verdict.signal -> ActionEvidence.cancel) end to end. The
  Movement clear counter proves only that the reload INVOKED the pending-clear hook; the clearing semantics themselves
  are pinned by MonitorSourceWitnessTest, not here.
- removeDirect calls the private 4-arg remove() UNDER Config.POLICY_LOCK, mirroring the production contract.
- Clocks: ONLY Verdict's System.currentTimeMillis() is rebound to TestClock (replacement count and source/fixture
  hashes asserted below; every other production source is byte-identical, hash-verified per file). Cases keeps the
  real clock; System.nanoTime (the combat log-only budget) is UNBOUND - S2 fires single combat-free signals, so no
  budget window is exercised.
- SINGLE-THREADED fixture: this proves nothing about concurrent reload/caller-action atomicity. That dimension is
  covered by Box's threaded signal+reload stress test, not here.
- This is a PARTIAL integration fixture, not the closed 0086 trio: no live server runtime, no concurrency, MC runtime
  doubled. Live-server bot runs remain spec-S territory (held).
Prereqs are HARD requirements: missing javac or Gson jar FAILS the test (exit != 0) - a skip that exits 0 masks a
never-run gate. Toolchain identity (javac version, exact Gson jar path + sha256) and a fixture source manifest hash
are printed on every run; subprocesses are timeout-bound and output-capped: RLIMIT_FSIZE is a PER-FILE limit on
ALL child writes (these logs AND javac's .class outputs alike), wall time is bounded by timeouts (the timeout kills
the direct child only - no process-group cleanup), and only seek-based bounded log tails are read back. preexec_fn
is POSIX-only. A dry control-flow run with declared tool doubles lives in test-policy-reload-integration-dry.sh."""
from pathlib import Path
import glob, hashlib, os, resource, shutil, subprocess, tempfile, unittest
R = Path(os.environ.get('MESHAC_TEST_TREE', Path(__file__).resolve().parents[1]))
S = R/'src/main/java/dev/meshac'
PROD = ['Config','Ladder','Verdict','Cases','SafeSwap','AdminLog','CheckNames','ActionEvidence','Vehicle']
CLOCK_NEEDLE = 'System.currentTimeMillis()'  # rebound in Verdict only; count asserted, not assumed
LOG_CAP = 16 * 1024 * 1024  # proposal: per-log RLIMIT_FSIZE cap; the child dies with SIGXFSZ on excess - a cap that actually caps

def bounded_tail(path, nbytes=4000):
    # seek-based tail: never reads more than nbytes into memory, whatever the file size
    with open(path, 'rb') as f:
        f.seek(0, os.SEEK_END); size = f.tell(); f.seek(max(0, size - nbytes))
        return f.read(nbytes).decode(errors='replace'), size

def gson_jar():
    if os.environ.get('GSON_JAR') and Path(os.environ['GSON_JAR']).exists(): return os.environ['GSON_JAR']
    for pat in [os.path.expanduser('~/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/*/*/gson-*.jar'),
                os.path.expanduser('~/meshac-work/**/gson-*.jar')]:
        hits = sorted(glob.glob(pat, recursive=True))
        if hits: return hits[-1]
    return None

def sha(b): return hashlib.sha256(b).hexdigest()

DOUBLES = {
'dev/meshac/Meshac.java': '''package dev.meshac; import java.util.*;
public class Meshac { public static Logger LOG = new Logger();
 public static class Logger { public final List<String> all = new ArrayList<>();
  public void warn(String s, Object... o) { all.add(s + " | " + Arrays.toString(o)); }
  public void info(String s, Object... o) { all.add(s + " | " + Arrays.toString(o)); }
  public boolean containsSince(String sub, int mark) { for (int i = mark; i < all.size(); i++) if (all.get(i).contains(sub)) return true; return false; } } }''',
'dev/meshac/Movement.java': '''package dev.meshac; public class Movement { public static int clears; public static void clearPendingActions() { clears++; } }''',
'dev/meshac/Discord.java': '''package dev.meshac; public class Discord { public static int posts, holds; public static void post(Cases.Case c) { posts++; } public static void hold(String a, String b, String c) { holds++; } }''',
'dev/meshac/Screens.java': '''package dev.meshac; public class Screens { public static Object kick(String r, Cases.Case c) { return "kick"; } public static Object ban(Cases.Case c) { return "ban"; } }''',
'dev/meshac/TestClock.java': '''package dev.meshac; public class TestClock { public static long ms = 1000000; }''',
'dev/meshac/ActionTrace.java': '''package dev.meshac; import java.util.function.Supplier; import net.minecraft.server.level.ServerPlayer; public class ActionTrace { public static void event(ServerPlayer p, String s, Supplier<String> x) {} }''',
'dev/meshac/Packets.java': '''package dev.meshac; public class Packets { public static Pos pos(Object p) { return new Pos(); } public static class Pos { public double y = 1; } }''',
'dev/meshac/veil/Veil.java': '''package dev.meshac.veil; public class Veil { public static boolean xrayOn, espOn, bedrockOn; public static void clear() {} public static void startupCheck() {} }''',
'dev/meshac/veil/VeilSettings.java': '''package dev.meshac.veil; import java.util.*;
public class VeilSettings { public VeilSettings(Boolean a, Boolean b, Boolean c, Boolean d) {} public String describe() { return ""; } public static List<String> typos(Set<String> keys) { return List.of(); } }''',
'net/fabricmc/loader/api/FabricLoader.java': '''package net.fabricmc.loader.api; import java.nio.file.*;
public class FabricLoader { public static FabricLoader getInstance() { return new FabricLoader(); } public Path getConfigDir() { return Path.of(System.getProperty("meshac.test.configdir")); } }''',
'net/minecraft/server/level/ServerPlayer.java': '''package net.minecraft.server.level; import java.util.UUID;
public class ServerPlayer { public final UUID id = UUID.randomUUID(); public UUID getUUID() { return id; }
 public record Profile(String name) {} public Profile getGameProfile() { return new Profile("test"); }
 public boolean isCreative() { return false; } public boolean isSpectator() { return false; }
 public Conn connection = new Conn(); public static class Conn { public int disconnects; public void disconnect(Object o) { disconnects++; } }
 public net.minecraft.world.entity.Entity vehicle = new net.minecraft.world.entity.vehicle.boat.AbstractBoat();
 public net.minecraft.world.entity.Entity getRootVehicle() { return vehicle; }
 public Level level() { return new Level(); }
 public static class Level { public Block getBlockState(net.minecraft.core.BlockPos p) { return new Block(); } public Block getFluidState(net.minecraft.core.BlockPos p) { return new Block(); }
  public static class Block { public boolean is(Object o) { return false; } }
  public Server getServer() { return new Server(); } }
 public static class Server { public static int tick; public int getTickCount() { return tick; } public Players getPlayerList() { return new Players(); } }
 public static class Players { public ServerPlayer getPlayer(UUID u) { return null; } }
 public void sendSystemMessage(Object o) {} }''',
'net/minecraft/world/entity/Entity.java': '''package net.minecraft.world.entity;
public class Entity { public int getId() { return 7; } public double getY() { return 0; } public boolean onGround() { return false; } public boolean isInWater() { return false; }
 public net.minecraft.core.BlockPos blockPosition() { return new net.minecraft.core.BlockPos(); } public Object getDeltaMovement() { return null; }
 public net.minecraft.server.level.ServerPlayer.Level level() { return new net.minecraft.server.level.ServerPlayer.Level(); } }''',
'net/minecraft/world/entity/vehicle/boat/AbstractBoat.java': '''package net.minecraft.world.entity.vehicle.boat; public class AbstractBoat extends net.minecraft.world.entity.Entity {}''',
'net/minecraft/network/protocol/game/ServerboundMoveVehiclePacket.java': '''package net.minecraft.network.protocol.game; public class ServerboundMoveVehiclePacket {}''',
'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat; public class Component { public static Object literal(String s) { return s; } }''',
'net/minecraft/core/BlockPos.java': '''package net.minecraft.core; public class BlockPos { public BlockPos below() { return this; } }''',
'net/minecraft/world/level/block/Blocks.java': '''package net.minecraft.world.level.block; public class Blocks { public static Object BUBBLE_COLUMN = new Object(); }''',
'net/minecraft/tags/FluidTags.java': '''package net.minecraft.tags; public class FluidTags { public static Object WATER = new Object(); }''',
}

HARNESS = '''package dev.meshac;
import java.lang.reflect.*; import java.nio.file.*; import java.util.*; import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;

/** S1 first action after invalid reload. S2 REAL direct-cancel caller (Vehicle.check). S3 remove with invalid half-life. Single-threaded: no concurrency proof. */
public final class ReloadHarness {
 static int checks; static void ck(boolean b, String m) { checks++; if (!b) throw new AssertionError(m); }
 static final Path DIR = Path.of(System.getProperty("meshac.test.configdir"));
 static final String VALID_LOW = "{\\"holdAt\\":1,\\"removeAt\\":6,\\"halfLifeSeconds\\":1000000,\\"requireClusters\\":2,\\"requireClustersTierA\\":2,\\"offenceHalfLifeDays\\":30,\\"kicksBeforeTempban\\":3,\\"offenceMemoryDays\\":30}";
 static void writeCfg(String json) throws Exception { Files.writeString(DIR.resolve("meshac.json"), json); }
 static double lhHeat(ServerPlayer p) throws Exception {
  Field f = Verdict.class.getDeclaredField("HEAT"); f.setAccessible(true); Object h = ((Map<?,?>) f.get(null)).get(p.getUUID()); if (h == null) return 0;
  Field lh = h.getClass().getDeclaredField("lh"); lh.setAccessible(true); Field heat = Ladder.Heat.class.getDeclaredField("heat"); heat.setAccessible(true); return heat.getDouble(lh.get(h)); }
 static int heatSize() throws Exception { Field f = Verdict.class.getDeclaredField("HEAT"); f.setAccessible(true); return ((Map<?,?>) f.get(null)).size(); }
 static void removeDirect(ServerPlayer p, String why) throws Exception {
  Method m = Verdict.class.getDeclaredMethod("remove", ServerPlayer.class, String.class, List.class, Config.class); m.setAccessible(true);
  synchronized (Config.POLICY_LOCK) { m.invoke(null, p, why, List.of("harness"), Config.get()); } } // production calls remove() only under the lock; mirror that contract
 static boolean[] rise5(ServerPlayer p) { boolean[] r = new boolean[5]; for (int i = 0; i < 5; i++) { ServerPlayer.Server.tick++; r[i] = Vehicle.check(p, new ServerboundMoveVehiclePacket()); } return r; } // Vehicle RISE_AT = 5: a refusal can only land on the 5th rise of a run
 static void expectRefusal(String phase, boolean[] r, boolean fifth) { // calls 1-4 must NEVER refuse (rise budget not matured); call 5 carries the verdict
  for (int i = 0; i < 4; i++) ck(!r[i], phase + ": call " + (i+1) + " refused before the rise budget matured");
  ck(r[4] == fifth, phase + ": call 5 refusal = " + r[4] + ", expected " + fifth); }

 public static void main(String[] a) throws Exception {
  // S1: first action after an invalid reload
  writeCfg(VALID_LOW); ck(Config.load() == AdminLog.ReloadResult.APPLIED, "S1 valid load not APPLIED"); long e1 = Config.current().epoch;
  ck(Config.get().enforce("interact"), "S1 valid config should enforce");
  ServerPlayer p = new ServerPlayer();
  TestClock.ms += 200; Verdict.Step s1 = Verdict.signal(p, "fly", "harness", 3);
  ck(s1 == Verdict.Step.HOLD, "S1 first valid weight-3 expected HOLD, got " + s1);
  int m1 = Meshac.LOG.all.size();
  writeCfg("{\\"halfLifeSeconds\\":-1}"); ck(Config.load() == AdminLog.ReloadResult.APPLIED, "S1 invalid load not APPLIED");
  ck(Config.current().epoch > e1, "S1 epoch did not bump on invalid reload");
  ck(!Config.get().enforce("interact"), "S1 invalid policy must pause enforcement (log-only)");
  ck(Meshac.LOG.containsSince("invalid ladder config", m1), "S1 invalid-policy diagnostic missing since scenario mark");
  TestClock.ms += 200; Verdict.Step a1 = Verdict.signal(p, "fly", "harness", 3);
  ck(a1 == Verdict.Step.HOLD, "S1 invalid period, first burst under retained params expected HOLD, got " + a1);
  ck(lhHeat(p) > 2.9, "S1 invalid-period evidence must accumulate within its epoch (lh heat " + lhHeat(p) + ")");
  int m2 = Meshac.LOG.all.size();
  TestClock.ms += 200; Verdict.Step a2 = Verdict.signal(p, "fly", "harness", 3);
  ck(a2 == Verdict.Step.REMOVE, "S1 retained params should REACH the remove step, got " + a2);
  ck(p.connection.disconnects == 0 && Cases.of("test").isEmpty(), "S1 invalid policy produced an ACTION (disconnect or case)");
  ck(Meshac.LOG.containsSince("would be removed", m2), "S1 enforce-gate refusal log missing since scenario mark");
  writeCfg(VALID_LOW); Config.load();
  ck(Config.get().enforce("interact"), "S1 valid reload should re-enable enforcement");
  ck(heatSize() == 0, "S1 reload did not wipe HEAT (fail-closed evidence loss)");
  TestClock.ms += 200; Verdict.Step b1 = Verdict.signal(p, "fly", "harness", 3);
  ck(b1 == Verdict.Step.HOLD, "S1 first flag after valid reload must be HOLD (fresh evidence), got " + b1);
  TestClock.ms += 200; Verdict.Step b2 = Verdict.signal(p, "fly", "harness", 3);
  ck(b2 == Verdict.Step.REMOVE && p.connection.disconnects == 1 && Cases.of("test").size() == 1, "S1 enforcement not restored after valid reload: " + b2 + " dc=" + p.connection.disconnects);
  ck(Discord.posts == 1, "S1 removal should post exactly one case to Discord, got " + Discord.posts);

  // S2: the REAL MC direct-cancel caller: Vehicle.check -> Verdict.signal -> ActionEvidence.cancel
  writeCfg(VALID_LOW); Config.load();
  ServerPlayer r = new ServerPlayer();
  expectRefusal("S2 enforcing", rise5(r), true); // boatfly rise must be REFUSED by the real caller
  int m3 = Meshac.LOG.all.size();
  synchronized (Config.POLICY_LOCK) { Config.get().monitorOnly = true; Movement.clearPendingActions(); Verdict.clearPendingActions(); } // the exact /mesh monitor-on commit shape
  expectRefusal("S2 monitor-on", rise5(r), false); // the real caller must NOT refuse
  ck(Meshac.LOG.containsSince("SIGNAL", m3) && Meshac.LOG.containsSince("log", m3), "S2 monitor signals must still be logged");
  synchronized (Config.POLICY_LOCK) { Config.get().monitorOnly = false; }
  ck(Config.get().enforce("boatfly"), "S2 monitor-off must REOPEN the gate");
  expectRefusal("S2 monitor-off", rise5(r), true); // refusal restored through the real caller
  int clears = Movement.clears; int m4 = Meshac.LOG.all.size();
  writeCfg("{\\"halfLifeSeconds\\":-1}"); Config.load();
  ck(Movement.clears > clears, "S2 reload did not invoke the Movement pending-clear hook"); // invocation only: clearing semantics are pinned by MonitorSourceWitnessTest
  ck(!Config.get().enforce("boatfly"), "S2 invalid policy must close the direct gate");
  expectRefusal("S2 invalid-policy", rise5(r), false); // the real caller signals but must NOT refuse
  ck(Meshac.LOG.containsSince("boatfly", m4), "S2 invalid-policy signals must still be logged");

  // S3: remove with an invalid offence half-life
  int m5 = Meshac.LOG.all.size();
  writeCfg("{\\"offenceHalfLifeDays\\":-5}"); Config.load();
  ck(Meshac.LOG.containsSince("invalid offenceHalfLifeDays", m5), "S3 half-life diagnostic missing since scenario mark");
  ck(!Config.get().enforce("fly"), "S3 invalid half-life policy must pause enforcement");
  ServerPlayer q = new ServerPlayer();
  int before = Cases.of("test").size();
  removeDirect(q, "fly harness");
  ck(q.connection.disconnects == 0 && Cases.of("test").size() == before, "S3 remove() under an invalid policy must refuse");
  writeCfg(VALID_LOW); Config.load();
  TestClock.ms += 200; Verdict.signal(q, "fly", "harness", 3);
  TestClock.ms += 200; Verdict.Step rr = Verdict.signal(q, "fly", "harness", 3);
  ck(rr == Verdict.Step.REMOVE && q.connection.disconnects == 1, "S3 removal after valid reload failed: " + rr);
  ck(Cases.of("test").size() == before + 1 && Cases.of("test").get(before).action.equals("kick"), "S3 expected a kick case at rung 1");
  double eff = Cases.offences(q.getUUID()); ck(eff == eff && eff > 0.9 && eff <= 1.0, "S3 offences not finite/expected: " + eff);
  System.out.println("ReloadHarness PASS (" + checks + " checks)");
 }
}
'''

class PolicyReloadIntegration(unittest.TestCase):
    def run_logged(self, cmd, log, timeout):
        """Output-capped subprocess. RLIMIT_FSIZE=LOG_CAP is a PER-FILE limit on every write the child makes:
        it caps this log AND any other file the child writes (javac's .class outputs included - a single output
        file over the cap would kill a real compile, which is why the cap is 16 MiB and not smaller). The timeout
        bounds wall time but kills the direct child only (no process-group cleanup; neither javac nor the harness
        java forks here). Only a seek-based bounded tail of the log is read back - no whole-log reads. Detection
        is size-based, not signal-based: children that ignore SIGXFSZ (CPython does; it raises EFBIG instead)
        still cannot grow a file past the cap, so a log at cap size means truncated output and fails the test.
        preexec_fn is POSIX-only. Verified locally: a child attempting 8 MB under a 1 MB cap leaves exactly
        1048576 bytes; an under-cap child completes rc=0."""
        with open(log, 'wb') as lf:
            try:
                r = subprocess.run(cmd, stdout=lf, stderr=subprocess.STDOUT, timeout=timeout,
                                   preexec_fn=lambda: resource.setrlimit(resource.RLIMIT_FSIZE, (LOG_CAP, LOG_CAP)))
            except subprocess.TimeoutExpired:
                self.fail(f'{cmd[0]} exceeded the {timeout} s bound')
        tail, size = bounded_tail(log)
        print(f'{cmd[0]} log {size} bytes (cap {LOG_CAP}) at {log}')
        if size >= LOG_CAP:
            self.fail(f'{cmd[0]} hit the {LOG_CAP}-byte output cap - log truncated (rc={r.returncode}); cap-brushing output is never a pass')
        return r.returncode, tail

    def test_reload_integration(self):
        gj = gson_jar()
        self.assertTrue(shutil.which('javac'), 'HARD prereq missing: javac not on PATH - this test must RUN, not skip (Box provides the JDK)')
        self.assertIsNotNone(gj, 'HARD prereq missing: no Gson jar (set GSON_JAR or use the gradle cache) - this test must RUN, not skip')
        self.assertTrue(Path(gj).name.startswith('gson-'), f'gson jar sanity: {gj}')
        print('TOOLCHAIN gson jar:', gj, 'sha256', sha(Path(gj).read_bytes()))
        with tempfile.TemporaryDirectory(prefix='reload-it-') as d:
            root = Path(d); cfgdir = root/'config'; cfgdir.mkdir()
            rc, jvt = self.run_logged(['javac','-version'], root/'javac-version.log', 30)
            self.assertEqual(rc, 0, 'javac -version failed:\n' + jvt)
            rc, vvt = self.run_logged(['java','-version'], root/'java-version.log', 30)
            self.assertEqual(rc, 0, 'java -version failed:\n' + vvt)
            print('TOOLCHAIN javac:', shutil.which('javac'), jvt.strip().splitlines()[0])
            print('TOOLCHAIN java:', shutil.which('java'), vvt.strip().splitlines()[0])
            sources, manifest = [], []
            for rel, text in sorted(DOUBLES.items()):
                f = root/rel; f.parent.mkdir(parents=True, exist_ok=True); f.write_text(text); sources.append(str(f))
                manifest.append(f'{rel}:{sha(text.encode())}')
            for n in PROD:
                text = (S/f'{n}.java').read_text()
                if n == 'Verdict':
                    n_clock = text.count(CLOCK_NEEDLE)
                    self.assertEqual(n_clock, 5, 'expected exactly 5 clock sites in production Verdict (update the pin on a real change; 0 would mean the rebinding silently no-ops)')
                    text = text.replace(CLOCK_NEEDLE, 'dev.meshac.TestClock.ms')
                    self.assertEqual(text.count(CLOCK_NEEDLE), 0, 'clock rebinding incomplete')
                    self.assertEqual(text.count('dev.meshac.TestClock.ms'), n_clock, 'clock rebinding count drifted')
                    self.assertEqual(text.replace('dev.meshac.TestClock.ms', CLOCK_NEEDLE), (S/f'{n}.java').read_text(), 'Verdict fixture copy differs from production beyond the clock rebinding')
                    print(f'SOURCE Verdict.java: clock rebound at {n_clock} sites, reversal-identical')
                f = root/'prod'/f'dev/meshac/{n}.java'; f.parent.mkdir(parents=True, exist_ok=True); f.write_text(text); sources.append(str(f))
                src_b, fix_b = (S/f'{n}.java').read_bytes(), f.read_bytes()
                manifest.append(f'prod/dev/meshac/{n}.java:source={sha(src_b)}:fixture={sha(fix_b)}')
                if n == 'Verdict':
                    print(f'SOURCE Verdict.java source sha256 {sha(src_b)} fixture sha256 {sha(fix_b)} (differs ONLY by the clock rebinding; reversal + count asserted)')
                else:
                    self.assertEqual(src_b, fix_b, f'{n}.java fixture bytes differ from production bytes')
                    print(f'SOURCE {n}.java sha256 {sha(src_b)} (byte-identical, fixture bytes re-hashed from disk)')
            h = root/'dev/meshac/ReloadHarness.java'; h.write_text(HARNESS); sources.append(str(h))
            manifest.append(f'dev/meshac/ReloadHarness.java:{sha(HARNESS.encode())}')
            mhash = hashlib.sha256('\n'.join(manifest).encode()).hexdigest()
            print('FIXTURE-MANIFEST sha256', mhash)
            for row in manifest: print('MANIFEST-ROW', row)
            out = root/'out'
            rc, jtail = self.run_logged(['javac','-J-Xmx256m','-cp',gj,'-d',str(out)]+sources, root/'javac.log', 300)
            self.assertEqual(rc, 0, 'javac failed, bounded tail:\n' + jtail)
            rc, htail = self.run_logged(['java','-Xmx256m',f'-Dmeshac.test.configdir={cfgdir}','-cp',f'{out}:{gj}','dev.meshac.ReloadHarness'], root/'harness.log', 180)
            self.assertEqual(rc, 0, f'harness rc={rc}, bounded tail:\n' + htail)
            self.assertIn('ReloadHarness PASS', htail)
            print(htail.strip())

if __name__ == '__main__': unittest.main()
