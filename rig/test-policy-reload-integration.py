#!/usr/bin/env python3
"""0096: the three reviewer-owed MC-side integration tests (0086) against the REAL reload path.
Real production classes: Config, Ladder, Verdict, Cases, SafeSwap, AdminLog, CheckNames, ActionEvidence - plus a REAL
Gson jar (the reload path is half Gson; the monitor-invariant doubles fake it). Minecraft runtime is service doubles
(ServerPlayer, FabricLoader, Movement, Discord, Screens, veil). Verdict's wall clock is rebound to TestClock (same
textual rebinding the monitor-invariant harness uses); everything else is unchanged production source.
Scenarios: S1 first action after an invalid reload, S2 the direct cancel path follows the enforce() gate,
S3 remove with an invalid offence half-life. Needs javac + a Gson jar (GSON_JAR env or the gradle cache): Box runs it."""
from pathlib import Path
import glob, os, shutil, subprocess, tempfile, unittest
R = Path(os.environ.get('MESHAC_TEST_TREE', Path(__file__).resolve().parents[1]))
S = R/'src/main/java/dev/meshac'

def gson_jar():
    if os.environ.get('GSON_JAR') and Path(os.environ['GSON_JAR']).exists(): return os.environ['GSON_JAR']
    for pat in [os.path.expanduser('~/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/*/*/gson-*.jar'),
                os.path.expanduser('~/meshac-work/**/gson-*.jar')]:
        hits = sorted(glob.glob(pat, recursive=True))
        if hits: return hits[-1]
    return None

DOUBLES = {
'dev/meshac/Meshac.java': '''package dev.meshac; import java.util.*;
public class Meshac { public static Logger LOG = new Logger();
 public static class Logger { public final List<String> all = new ArrayList<>();
  public void warn(String s, Object... o) { all.add(s + " | " + Arrays.toString(o)); }
  public void info(String s, Object... o) { all.add(s + " | " + Arrays.toString(o)); }
  public boolean contains(String sub) { for (String l : all) if (l.contains(sub)) return true; return false; } } }''',
'dev/meshac/Movement.java': '''package dev.meshac; public class Movement { public static int clears; public static void clearPendingActions() { clears++; } }''',
'dev/meshac/Discord.java': '''package dev.meshac; public class Discord { public static int posts, holds; public static void post(Cases.Case c) { posts++; } public static void hold(String a, String b, String c) { holds++; } }''',
'dev/meshac/Screens.java': '''package dev.meshac; public class Screens { public static Object kick(String r, Cases.Case c) { return "kick"; } public static Object ban(Cases.Case c) { return "ban"; } }''',
'dev/meshac/TestClock.java': '''package dev.meshac; public class TestClock { public static long ms = 1000000; }''',
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
 public Level level() { return new Level(); } public static class Level { public Server getServer() { return new Server(); } }
 public static class Server { public Players getPlayerList() { return new Players(); } }
 public static class Players { public ServerPlayer getPlayer(UUID u) { return null; } }
 public void sendSystemMessage(Object o) {} }''',
'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat; public class Component { public static Object literal(String s) { return s; } }''',
}

HARNESS = '''package dev.meshac;
import java.lang.reflect.*; import java.nio.file.*; import java.util.*; import net.minecraft.server.level.ServerPlayer;

/** S1 first action after invalid reload. S2 direct cancel path follows enforce(). S3 remove with invalid half-life. */
public final class ReloadHarness {
 static int checks; static void ck(boolean b, String m) { checks++; if (!b) throw new AssertionError(m); }
 static final Path DIR = Path.of(System.getProperty("meshac.test.configdir"));
 static final String VALID_LOW = "{\\"holdAt\\":1,\\"removeAt\\":6,\\"halfLifeSeconds\\":1000000,\\"requireClusters\\":2,\\"requireClustersTierA\\":2,\\"offenceHalfLifeDays\\":30,\\"kicksBeforeTempban\\":3,\\"offenceMemoryDays\\":30}";
 static void writeCfg(String json) throws Exception { Files.writeString(DIR.resolve("meshac.json"), json); }
 static double lhHeat(ServerPlayer p) throws Exception {
  Field f = Verdict.class.getDeclaredField("HEAT"); f.setAccessible(true); Object h = ((Map<?,?>) f.get(null)).get(p.getUUID()); if (h == null) return 0;
  Field lh = h.getClass().getDeclaredField("lh"); lh.setAccessible(true); Field heat = Ladder.Heat.class.getDeclaredField("heat"); heat.setAccessible(true); return heat.getDouble(lh.get(h)); }
 static int heatSize() throws Exception { Field f = Verdict.class.getDeclaredField("HEAT"); f.setAccessible(true); return ((Map<?,?>) f.get(null)).size(); }
 static void removeDirect(ServerPlayer p, String why) throws Exception { Method m = Verdict.class.getDeclaredMethod("remove", ServerPlayer.class, String.class, List.class, Config.class); m.setAccessible(true); m.invoke(null, p, why, List.of("harness"), Config.get()); }

 public static void main(String[] a) throws Exception {
  // S1: first action after an invalid reload
  writeCfg(VALID_LOW); ck(Config.load() == AdminLog.ReloadResult.APPLIED, "S1 valid load not APPLIED"); long e1 = Config.current().epoch;
  ck(Config.get().enforce("interact"), "S1 valid config should enforce");
  ServerPlayer p = new ServerPlayer();
  TestClock.ms += 200; Verdict.Step s1 = Verdict.signal(p, "fly", "harness", 3);
  ck(s1 == Verdict.Step.HOLD, "S1 first valid weight-3 expected HOLD, got " + s1);
  writeCfg("{\\"halfLifeSeconds\\":-1}"); ck(Config.load() == AdminLog.ReloadResult.APPLIED, "S1 invalid load not APPLIED");
  ck(Config.current().epoch > e1, "S1 epoch did not bump on invalid reload");
  ck(!Config.get().enforce("interact"), "S1 invalid policy must pause enforcement (log-only)");
  ck(Meshac.LOG.contains("invalid ladder config"), "S1 invalid-policy diagnostic missing");
  TestClock.ms += 200; Verdict.Step a1 = Verdict.signal(p, "fly", "harness", 3);
  ck(a1 == Verdict.Step.HOLD, "S1 invalid period, first burst under retained params expected HOLD, got " + a1);
  ck(lhHeat(p) > 2.9, "S1 invalid-period evidence must accumulate within its epoch (lh heat " + lhHeat(p) + ")");
  TestClock.ms += 200; Verdict.Step a2 = Verdict.signal(p, "fly", "harness", 3);
  ck(a2 == Verdict.Step.REMOVE, "S1 retained params should REACH the remove step, got " + a2);
  ck(p.connection.disconnects == 0 && Cases.of("test").isEmpty(), "S1 invalid policy produced an ACTION (disconnect or case)");
  ck(Meshac.LOG.contains("would be removed"), "S1 enforce-gate refusal log missing");
  writeCfg(VALID_LOW); Config.load();
  ck(Config.get().enforce("interact"), "S1 valid reload should re-enable enforcement");
  ck(heatSize() == 0, "S1 reload did not wipe HEAT (fail-closed evidence loss)");
  TestClock.ms += 200; Verdict.Step b1 = Verdict.signal(p, "fly", "harness", 3);
  ck(b1 == Verdict.Step.HOLD, "S1 first flag after valid reload must be HOLD (fresh evidence), got " + b1);
  TestClock.ms += 200; Verdict.Step b2 = Verdict.signal(p, "fly", "harness", 3);
  ck(b2 == Verdict.Step.REMOVE && p.connection.disconnects == 1 && Cases.of("test").size() == 1, "S1 enforcement not restored after valid reload: " + b2 + " dc=" + p.connection.disconnects);
  ck(Discord.posts == 1, "S1 removal should post exactly one case to Discord, got " + Discord.posts);

  // S2: the direct cancel path follows the same enforce() gate as verdicts
  ck(ActionEvidence.cancel(true, false) && !ActionEvidence.cancel(false, false) && !ActionEvidence.cancel(true, true), "S2 ActionEvidence.cancel semantics");
  synchronized (Config.POLICY_LOCK) { Config.get().monitorOnly = true; Movement.clearPendingActions(); Verdict.clearPendingActions(); } // the exact /mesh monitor-on commit shape
  ck(!Config.get().enforce("interact"), "S2 monitor-on must close the direct gate");
  TestClock.ms += 200; ck(Verdict.signal(p, "interact", "harness", 1) == Verdict.Step.LOG, "S2 monitor signal must be LOG");
  ck(!ActionEvidence.cancel(Config.get().enforce("interact"), false), "S2 caller cancel must follow the gate (monitor)");
  synchronized (Config.POLICY_LOCK) { Config.get().monitorOnly = false; }
  int clears = Movement.clears;
  writeCfg("{\\"offenceHalfLifeDays\\":-5}"); Config.load();
  ck(!Config.get().enforce("interact"), "S2 invalid policy must close the direct gate");
  ck(!ActionEvidence.cancel(Config.get().enforce("interact"), false), "S2 caller cancel must follow the gate (invalid policy)");
  ck(Movement.clears > clears, "S2 reload did not run the Movement pending-clear");

  // S3: remove with an invalid offence half-life (the -5 policy from S2 is still live)
  ck(Meshac.LOG.contains("invalid offenceHalfLifeDays"), "S3 half-life diagnostic missing");
  ServerPlayer q = new ServerPlayer();
  int before = Cases.of("test").size();
  removeDirect(q, "fly harness");
  ck(q.connection.disconnects == 0 && Cases.of("test").size() == before, "S3 remove() under an invalid policy must refuse");
  writeCfg(VALID_LOW); Config.load();
  TestClock.ms += 200; Verdict.signal(q, "fly", "harness", 3);
  TestClock.ms += 200; Verdict.Step r = Verdict.signal(q, "fly", "harness", 3);
  ck(r == Verdict.Step.REMOVE && q.connection.disconnects == 1, "S3 removal after valid reload failed: " + r);
  ck(Cases.of("test").size() == before + 1 && Cases.of("test").get(before).action.equals("kick"), "S3 expected a kick case at rung 1");
  double eff = Cases.offences(q.getUUID()); ck(eff == eff && eff > 0.9 && eff <= 1.0, "S3 offences not finite/expected: " + eff);
  System.out.println("ReloadHarness PASS (" + checks + " checks)");
 }
}
'''

class PolicyReloadIntegration(unittest.TestCase):
    def test_reload_integration(self):
        gj = gson_jar()
        if not shutil.which('javac') or not gj:
            self.skipTest('needs javac and a Gson jar (GSON_JAR or gradle cache) - Box runs this')
        with tempfile.TemporaryDirectory(prefix='reload-it-') as d:
            root = Path(d); cfgdir = root/'config'; cfgdir.mkdir()
            prod = ['Config','Ladder','Verdict','Cases','SafeSwap','AdminLog','CheckNames','ActionEvidence']
            sources = []
            for rel, text in DOUBLES.items():
                f = root/rel; f.parent.mkdir(parents=True, exist_ok=True); f.write_text(text); sources.append(str(f))
            for n in prod:
                text = (S/f'{n}.java').read_text()
                if n == 'Verdict': text = text.replace('System.currentTimeMillis()', 'dev.meshac.TestClock.ms')  # same rebinding as the monitor-invariant harness
                f = root/'prod'/f'dev/meshac/{n}.java'; f.parent.mkdir(parents=True, exist_ok=True); f.write_text(text); sources.append(str(f))
            h = root/'dev/meshac/ReloadHarness.java'; h.write_text(HARNESS); sources.append(str(h))
            out = root/'out'
            subprocess.run(['javac','-J-Xmx256m','-cp',gj,'-d',str(out)]+sources, check=True)
            r = subprocess.run(['java','-Xmx256m',f'-Dmeshac.test.configdir={cfgdir}','-cp',f'{out}:{gj}','dev.meshac.ReloadHarness'], capture_output=True, text=True)
            self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
            self.assertIn('ReloadHarness PASS', r.stdout)

if __name__ == '__main__': unittest.main()
