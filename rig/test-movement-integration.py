#!/usr/bin/env python3
"""0107: PARTIAL MC-side integration fixture for Movement - standing queue item 3 (MC-side integration tests for the
remaining unpinned paths: U1 nofall window, U2 powder band, U4 classify entry+eviction, U7 microhop + antiknockback).
Real production classes: Movement and its full MC-free helper closure plus the Verdict/Config/Ladder/Cases/AdminLog/
SafeSwap/ActionEvidence/CheckNames policy stack - compiled with a REAL Gson jar. The Minecraft runtime is service
doubles (scriptable Level: support/ceiling/powder/partial-shape answers are SCRIPTED PER PHASE, not physically
derived - stated honestly, this is a logic fixture, not a physics sim). Scope labels:
- Clocks: Movement's System.currentTimeMillis() (26 sites) and System.nanoTime() (6 sites) are rebound to TestClock,
  and Verdict's System.currentTimeMillis() (5 sites, same as 0097) - replacement counts pinned below; every other
  production source is byte-identical, hash-verified per file. BlinkClock internals and Verdict's nanoTime (combat
  budget) stay on the real clock: the scenarios exercise neither (small displacements, no combat signals).
- SINGLE-THREADED fixture: nothing here proves concurrent-tick/caller atomicity (Box's threaded stress owns that).
- TRACE is OFF (MESHAC_TRACE unset): trace-only evidence lines are NOT exercised; the classify proof uses the
  non-trace-gated [groundtol] tolerated line. Doubled Glide/Interact/Mining/BounceBlocks/StuckBlocks no-op or return
  scripted constants; their real internals are NOT covered. No live deploy; this is rig-only.
Prereqs are HARD: missing javac or Gson jar FAILS the test. Toolchain identity + fixture manifest hash printed on
every run; subprocesses timeout-bound, output-capped (RLIMIT_FSIZE, per-file, covers .class outputs too), bounded
seek-based log tails only; preexec_fn is POSIX-only. Dry control-flow run with declared tool doubles lives in
test-movement-integration-dry.sh."""
from pathlib import Path
import glob, hashlib, os, resource, shutil, subprocess, tempfile, unittest
R = Path(os.environ.get('MESHAC_TEST_TREE', Path(__file__).resolve().parents[1]))
S = R/'src/main/java/dev/meshac'
PROD = ['Movement','Verdict','Cases','Config','Ladder','AdminLog','SafeSwap','ActionEvidence','CheckNames',
        'ContextLogBudget','CompareLogBudget','BlinkOrigin','BlinkClock','BounceTracker','MediumExit',
        'StepAccumulator','MovementLifetime','MonitorGate','FpGuards','GroundEvidence','GroundTolerance',
        'BoostWindow','GlideWindow','GroundTrace','SustainBudget','StepEvidence','MoveContext']
CLOCK_MS = 'System.currentTimeMillis()'   # rebound in Movement (26 sites) and Verdict (5 sites); counts pinned
CLOCK_NS = 'System.nanoTime()'            # rebound in Movement only (6 sites)
PIN = {'Movement': (26, 6), 'Verdict': (5, None)}  # (ms sites rebound, ns sites rebound); Verdict's 2 nanoTime sites (combat log-only budget) stay on the real clock: no combat signals here, same as 0097
LOG_CAP = 16 * 1024 * 1024  # proposal: per-log RLIMIT_FSIZE cap; the child dies with SIGXFSZ on excess

def bounded_tail(path, nbytes=4000):
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
 public static boolean skip(net.minecraft.server.level.ServerPlayer p) { return false; }
 public static class Logger { public final List<String> all = new ArrayList<>();
  public void warn(String s, Object... o) { all.add(s + " | " + Arrays.toString(o)); }
  public void info(String s, Object... o) { all.add(s + " | " + Arrays.toString(o)); }
  public void debug(String s, Object... o) { all.add(s + " | " + Arrays.toString(o)); }
  public void error(String s, Object... o) { all.add(s + " | " + Arrays.toString(o)); }
  public boolean containsSince(String sub, int mark) { for (int i = mark; i < all.size(); i++) if (all.get(i).contains(sub)) return true; return false; } } }''',
'dev/meshac/Glide.java': '''package dev.meshac; public class Glide { public static void wind(net.minecraft.server.level.ServerPlayer p) {} }''',
'dev/meshac/Interact.java': '''package dev.meshac; public class Interact { public static void look(net.minecraft.server.level.ServerPlayer p, float yaw, float pitch) {} }''',
'dev/meshac/Mining.java': '''package dev.meshac; public class Mining { public static void look(net.minecraft.server.level.ServerPlayer p, float yaw, float pitch) {} }''',
'dev/meshac/BounceBlocks.java': '''package dev.meshac; public class BounceBlocks { public static double restitution(net.minecraft.world.level.Level l, double x, double y, double z) { return 0.0; } }''',
'dev/meshac/StuckBlocks.java': '''package dev.meshac; public class StuckBlocks {
 public static boolean cobweb(net.minecraft.world.level.Level l, net.minecraft.world.phys.AABB b) { return false; }
 public static boolean honeySlide(net.minecraft.world.level.Level l, net.minecraft.world.phys.AABB b, double x, double y, double z, double w, boolean g) { return false; }
 public static boolean powderBody(net.minecraft.world.level.Level l, net.minecraft.world.phys.AABB b) { return net.minecraft.world.level.Level.scriptPowderBody; } }''',
'dev/meshac/Discord.java': '''package dev.meshac; public class Discord { public static int posts, holds; public static void post(Cases.Case c) { posts++; } public static void hold(String a, String b, String c) { holds++; } }''',
'dev/meshac/Screens.java': '''package dev.meshac; public class Screens { public static Object kick(String r, Cases.Case c) { return "kick"; } public static Object ban(Cases.Case c) { return "ban"; } }''',
'dev/meshac/TestClock.java': '''package dev.meshac; public class TestClock { public static long ms = 1000000; public static long ns = 1000000; }''',
'dev/meshac/ActionTrace.java': '''package dev.meshac; import java.util.function.Supplier; import net.minecraft.server.level.ServerPlayer; public class ActionTrace { public static void event(ServerPlayer p, String s, Supplier<String> x) {} }''',
'dev/meshac/veil/Veil.java': '''package dev.meshac.veil; public class Veil { public static boolean xrayOn, espOn, bedrockOn; public static void clear() {} public static void startupCheck() {} }''',
'dev/meshac/veil/VeilSettings.java': '''package dev.meshac.veil; import java.util.*;
public class VeilSettings { public VeilSettings(Boolean a, Boolean b, Boolean c, Boolean d) {} public String describe() { return ""; } public static List<String> typos(Set<String> keys) { return List.of(); } }''',
'net/fabricmc/loader/api/FabricLoader.java': '''package net.fabricmc.loader.api; import java.nio.file.*;
public class FabricLoader { public static FabricLoader getInstance() { return new FabricLoader(); } public Path getConfigDir() { return Path.of(System.getProperty("meshac.test.configdir")); } }''',
'net/minecraft/world/entity/Entity.java': '''package net.minecraft.world.entity;
public class Entity { public double x, y, z;
 public int getId() { return 7; } public double getX() { return x; } public double getY() { return y; } public double getZ() { return z; }
 public double distanceToSqr(Entity e) { double dx = x - e.x, dy = y - e.y, dz = z - e.z; return dx*dx + dy*dy + dz*dz; }
 public Object getDeltaMovement() { return null; } public net.minecraft.core.BlockPos blockPosition() { return net.minecraft.core.BlockPos.containing(x, y, z); } }''',
'net/minecraft/world/entity/LivingEntity.java': '''package net.minecraft.world.entity; public class LivingEntity extends Entity {}''',
'net/minecraft/world/entity/projectile/hurtingprojectile/windcharge/AbstractWindCharge.java': '''package net.minecraft.world.entity.projectile.hurtingprojectile.windcharge;
public class AbstractWindCharge extends net.minecraft.world.entity.Entity { public net.minecraft.world.entity.Entity getOwner() { return null; } }''',
'net/minecraft/world/entity/player/Abilities.java': '''package net.minecraft.world.entity.player; public class Abilities { public boolean mayfly, flying; }''',
'net/minecraft/world/effect/MobEffect.java': '''package net.minecraft.world.effect; public class MobEffect {}''',
'net/minecraft/world/effect/MobEffects.java': '''package net.minecraft.world.effect;
public class MobEffects { public static final MobEffect JUMP_BOOST = new MobEffect(), LEVITATION = new MobEffect(), SLOW_FALLING = new MobEffect(); }''',
'net/minecraft/world/effect/MobEffectInstance.java': '''package net.minecraft.world.effect; public class MobEffectInstance { public int getAmplifier() { return 0; } }''',
'net/minecraft/world/entity/ai/attributes/Attributes.java': '''package net.minecraft.world.entity.ai.attributes; public class Attributes { public static final Object MOVEMENT_EFFICIENCY = new Object(); }''',
'net/minecraft/world/damagesource/DamageSource.java': '''package net.minecraft.world.damagesource;
public class DamageSource { private final net.minecraft.world.entity.Entity direct, entity; private final String msg;
 public DamageSource(net.minecraft.world.entity.Entity d, net.minecraft.world.entity.Entity e, String m) { direct = d; entity = e; msg = m; }
 public net.minecraft.world.entity.Entity getDirectEntity() { return direct; } public net.minecraft.world.entity.Entity getEntity() { return entity; } public String getMsgId() { return msg; } }''',
'net/minecraft/world/damagesource/DamageSources.java': '''package net.minecraft.world.damagesource;
public class DamageSources { public DamageSource fall() { return new DamageSource(null, null, "fall"); } }''',
'net/minecraft/world/phys/AABB.java': '''package net.minecraft.world.phys;
public class AABB { public final double minX, minY, minZ, maxX, maxY, maxZ;
 public AABB(double x0, double y0, double z0, double x1, double y1, double z1) { minX=x0; minY=y0; minZ=z0; maxX=x1; maxY=y1; maxZ=z1; }
 public AABB move(double dx, double dy, double dz) { return new AABB(minX+dx, minY+dy, minZ+dz, maxX+dx, maxY+dy, maxZ+dz); }
 public AABB inflate(double dx, double dy, double dz) { return new AABB(minX-dx, minY-dy, minZ-dz, maxX+dx, maxY+dy, maxZ+dz); }
 public AABB deflate(double d) { return inflate(-d, -d, -d); } }''',
'net/minecraft/world/phys/shapes/VoxelShape.java': '''package net.minecraft.world.phys.shapes;
public class VoxelShape { private final double maxY; private final boolean empty;
 private VoxelShape(boolean e, double m) { empty = e; maxY = m; }
 public static VoxelShape empty() { return new VoxelShape(true, 0); } public static VoxelShape full() { return new VoxelShape(false, 1); } public static VoxelShape partial() { return new VoxelShape(false, 0.5); }
 public boolean isEmpty() { return empty; }
 public double min(net.minecraft.core.Direction.Axis a) { return 0; }
 public double max(net.minecraft.core.Direction.Axis a) { return a == net.minecraft.core.Direction.Axis.Y ? maxY : 1; } }''',
'net/minecraft/core/Direction.java': '''package net.minecraft.core; public class Direction { public enum Axis { X, Y, Z } }''',
'net/minecraft/core/BlockPos.java': '''package net.minecraft.core;
public class BlockPos { protected int x, y, z;
 public BlockPos(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
 public int getX() { return x; } public int getY() { return y; } public int getZ() { return z; }
 public BlockPos below() { return new BlockPos(x, y - 1, z); } public BlockPos below(int n) { return new BlockPos(x, y - n, z); }
 public static BlockPos containing(double x, double y, double z) { return new BlockPos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z)); }
 public static class MutableBlockPos extends BlockPos { public MutableBlockPos() { super(0, 0, 0); } public void set(int x, int y, int z) { this.x = x; this.y = y; this.z = z; } } }''',
'net/minecraft/world/level/block/Block.java': '''package net.minecraft.world.level.block;
public class Block { private final String key; public Block(String k) { key = k; }
 public float getFriction() { return 0.6f; } public float getSpeedFactor() { return 1.0f; } public String toString() { return key; } }''',
'net/minecraft/world/level/block/Blocks.java': '''package net.minecraft.world.level.block;
public class Blocks { public static final Block POWDER_SNOW = new Block("minecraft:powder_snow"), BUBBLE_COLUMN = new Block("minecraft:bubble_column"),
  SCAFFOLDING = new Block("minecraft:scaffolding"), SLIME_BLOCK = new Block("minecraft:slime_block"); }''',
'net/minecraft/tags/BlockTags.java': '''package net.minecraft.tags; public class BlockTags { public static final Object BEDS = new Object(); }''',
'net/minecraft/world/level/block/state/BlockState.java': '''package net.minecraft.world.level.block.state;
public class BlockState { private final net.minecraft.world.level.block.Block block; private final boolean air; private final net.minecraft.world.phys.shapes.VoxelShape shape;
 private BlockState(net.minecraft.world.level.block.Block b, boolean a, net.minecraft.world.phys.shapes.VoxelShape s) { block = b; air = a; shape = s; }
 public static BlockState air() { return new BlockState(new net.minecraft.world.level.block.Block("minecraft:air"), true, net.minecraft.world.phys.shapes.VoxelShape.empty()); }
 public static BlockState powder() { return new BlockState(net.minecraft.world.level.block.Blocks.POWDER_SNOW, false, net.minecraft.world.phys.shapes.VoxelShape.full()); }
 public static BlockState partial() { return new BlockState(new net.minecraft.world.level.block.Block("minecraft:snow_layer"), false, net.minecraft.world.phys.shapes.VoxelShape.partial()); }
 public boolean isAir() { return air; } public boolean is(Object o) { return o == block; }
 public net.minecraft.world.level.block.Block getBlock() { return block; }
 public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.Level l, net.minecraft.core.BlockPos p) { return shape; } }''',
'net/minecraft/core/registries/BuiltInRegistries.java': '''package net.minecraft.core.registries;
public class BuiltInRegistries { public static final Reg BLOCK = new Reg(); public static class Reg { public Object getKey(net.minecraft.world.level.block.Block b) { return b; } } }''',
'net/minecraft/world/level/Level.java': '''package net.minecraft.world.level; import java.util.*;
public class Level {
 public static boolean support, ceiling, solid, powderFeet, partialBelow, scriptPowderBody;
 public static double pcMinY = 70, pcMaxY = 71.8; // player box Y extent, kept in sync by ServerPlayer.setPos
 public static void reset() { support = true; ceiling = solid = powderFeet = partialBelow = scriptPowderBody = false; }
 private static final Object DIM = new Object();
 public boolean noCollision(net.minecraft.server.level.ServerPlayer p, net.minecraft.world.phys.AABB b) {
  if (b.maxY <= pcMinY + 1e-6) return !support;   // support strip under the feet
  if (b.minY >= pcMaxY - 1e-6) return !ceiling;   // ceiling probe above the head
  return !solid; }                                 // body/interior probes: air unless scripted solid
 public net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos pos) {
  int fy = (int) Math.floor(pcMinY);
  if (powderFeet && pos.getY() <= fy && pos.getY() >= fy - 2) return net.minecraft.world.level.block.state.BlockState.powder();
  if (partialBelow && pos.getY() == fy) return net.minecraft.world.level.block.state.BlockState.partial();
  return net.minecraft.world.level.block.state.BlockState.air(); }
 public Object dimension() { return DIM; }
 public <T> List<T> getEntitiesOfClass(Class<T> c, net.minecraft.world.phys.AABB b) { return new ArrayList<>(); }
 public Server getServer() { return new Server(); }
 public static class Server { public Players getPlayerList() { return new Players(); } }
 public static class Players { public net.minecraft.server.level.ServerPlayer getPlayer(UUID u) { return null; } } }''',
'net/minecraft/server/level/ServerPlayer.java': '''package net.minecraft.server.level; import java.util.UUID;
public class ServerPlayer extends net.minecraft.world.entity.Entity {
 public final UUID id = UUID.randomUUID(); public UUID getUUID() { return id; }
 public record Profile(String name) {} private final Profile profile; public ServerPlayer(String n) { profile = new Profile(n); } public Profile getGameProfile() { return profile; }
 public int hurtTime; public float fallDistance;
 public boolean blocking, sneaking, water, lava, climbable, passenger, fallFlying, autoSpin, ground;
 public boolean onGround() { return ground; }
 public net.minecraft.world.entity.player.Abilities abilities = new net.minecraft.world.entity.player.Abilities();
 public net.minecraft.world.entity.player.Abilities getAbilities() { return abilities; }
 public boolean isCreative() { return false; } public boolean isSpectator() { return false; }
 public boolean isPassenger() { return passenger; } public boolean isFallFlying() { return fallFlying; }
 public boolean isAutoSpinAttack() { return autoSpin; } public boolean isInWater() { return water; } public boolean isInLava() { return lava; }
 public boolean onClimbable() { return climbable; } public boolean isBlocking() { return blocking; } public boolean isShiftKeyDown() { return sneaking; }
 public float getHealth() { return 20.0f; } public boolean isAlive() { return true; }
 public float getSpeed() { return 0.1f; } public float getBbWidth() { return 0.6f; }
 private net.minecraft.world.effect.MobEffect[] fx = new net.minecraft.world.effect.MobEffect[0];
 public boolean hasEffect(net.minecraft.world.effect.MobEffect e) { for (net.minecraft.world.effect.MobEffect f : fx) if (f == e) return true; return false; }
 public net.minecraft.world.effect.MobEffectInstance getEffect(net.minecraft.world.effect.MobEffect e) { return null; }
 public double getAttributeValue(Object attr) { return 0.0; }
 public float yRot, xRot; public float getYRot() { return yRot; } public float getXRot() { return xRot; }
 public void setPos(double nx, double ny, double nz) { x = nx; y = ny; z = nz; net.minecraft.world.level.Level.pcMinY = ny; net.minecraft.world.level.Level.pcMaxY = ny + 1.8; }
 public net.minecraft.world.phys.AABB getBoundingBox() { return new net.minecraft.world.phys.AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3); }
 public net.minecraft.core.BlockPos blockPosition() { return net.minecraft.core.BlockPos.containing(x, y, z); }
 private final net.minecraft.world.level.Level level = new net.minecraft.world.level.Level(); public net.minecraft.world.level.Level level() { return level; }
 public net.minecraft.world.damagesource.DamageSource lastDamage; public net.minecraft.world.damagesource.DamageSource getLastDamageSource() { return lastDamage; }
 public net.minecraft.world.damagesource.DamageSources damageSources() { return new net.minecraft.world.damagesource.DamageSources(); }
 public int fallDamageCalls; public void causeFallDamage(double d, float m, net.minecraft.world.damagesource.DamageSource s) { fallDamageCalls++; }
 public void sendSystemMessage(Object o) {}
 public Conn connection = new Conn(); public static class Conn { public int disconnects; public void disconnect(Object o) { disconnects++; } } }''',
'net/minecraft/network/protocol/game/ServerboundMovePlayerPacket.java': '''package net.minecraft.network.protocol.game;
public class ServerboundMovePlayerPacket {
 private final double x, y, z; private final boolean pos, ground;
 private ServerboundMovePlayerPacket(double x, double y, double z, boolean g) { this.x = x; this.y = y; this.z = z; pos = true; ground = g; }
 public static ServerboundMovePlayerPacket pos(double x, double y, double z, boolean g) { return new ServerboundMovePlayerPacket(x, y, z, g); }
 public boolean hasPosition() { return pos; } public boolean hasRotation() { return false; } public boolean isOnGround() { return ground; }
 public double getX(double fb) { return pos ? x : fb; } public double getY(double fb) { return pos ? y : fb; } public double getZ(double fb) { return pos ? z : fb; }
 public float getYRot(float fb) { return fb; } public float getXRot(float fb) { return fb; } }''',
'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat; public class Component { public static Object literal(String s) { return s; } }''',
}

HARNESS = '''package dev.meshac;
import java.nio.file.*; import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/** Movement integration harness. World answers are SCRIPTED (Level statics) per phase - a logic fixture, not physics.
 * Fresh player per scenario (fresh Movement state + fresh Verdict heat). TRACE off: trace-only lines not exercised.
 * mode "classify" must be paired with env MESHAC_GROUND_TOL=classify (set by the fixture run); "plain" pairs with unset. */
public final class MovementHarness {
 static int checks; static void ck(boolean b, String m) { checks++; if (!b) throw new AssertionError(m); }
 static final java.nio.file.Path DIR = java.nio.file.Path.of(System.getProperty("meshac.test.configdir"));
 static final String VALID_LOW = "{\\"holdAt\\":1,\\"removeAt\\":6,\\"halfLifeSeconds\\":1000000,\\"requireClusters\\":2,\\"requireClustersTierA\\":2,\\"offenceHalfLifeDays\\":30,\\"kicksBeforeTempban\\":3,\\"offenceMemoryDays\\":30}";

 static void adv(long ms) { TestClock.ms += ms; TestClock.ns += ms * 1000000L; }
 static int mark() { return Meshac.LOG.all.size(); }
 static boolean since(String sub, int m) { return Meshac.LOG.containsSince(sub, m); }
 static ServerPlayer fresh(double y) { ServerPlayer p = new ServerPlayer("test"); p.setPos(0, y, 0); return p; }
 static double[] send(ServerPlayer p, double x, double y, double z, boolean g) {
  adv(50); double[] r = Movement.check(p, ServerboundMovePlayerPacket.pos(x, y, z, g));
  if (r != null) p.setPos(r[0], r[1], r[2]); else p.setPos(x, y, z); return r; }
 static void warmup(ServerPlayer p, double y) { for (int i = 0; i < 25; i++) send(p, 0, y, 0, true); } // 1 init + 20 grace burn + 4 scored SUPPORTED claims (Level.support still true)

 public static void main(String[] a) throws Exception {
  boolean classify = a.length > 0 && a[0].equals("classify");
  TestClock.ms = 1700000000000L; TestClock.ns = 1000000000L;
  Files.writeString(DIR.resolve("meshac.json"), VALID_LOW);
  ck(Config.load() == AdminLog.ReloadResult.APPLIED, "valid config not APPLIED");

  // S1 microhop (U7): two unsupported dy~0.1 ground claims inside the hop clock flag; one does not.
  net.minecraft.world.level.Level.reset(); ServerPlayer p1 = fresh(70); warmup(p1, 70);
  net.minecraft.world.level.Level.support = false; int m1 = mark();
  send(p1, 0, 70.1, 0, true); ck(!since("microhop", m1), "S1: one hop must be forgiven");
  send(p1, 0, 70.2, 0, true); ck(since("microhop", m1), "S1: two unsupported hops inside 30 packets must flag microhop");

  // S1b low ceiling: same hops under a ceiling are the sprint-jump-under-2-block legit case - never flagged.
  net.minecraft.world.level.Level.reset(); ServerPlayer p1b = fresh(70); warmup(p1b, 70);
  net.minecraft.world.level.Level.support = false; net.minecraft.world.level.Level.ceiling = true; int m1b = mark();
  send(p1b, 0, 70.1, 0, true); send(p1b, 0, 70.2, 0, true); send(p1b, 0, 70.3, 0, true);
  ck(!since("microhop", m1b), "S1b: low-ceiling hops must NOT flag microhop (the nofall signal these claims earn is separate and expected)");

  // S2 antiknockback (U7): melee hurt + no movement 450+ ms later, three times in a row, via the real tick() path.
  net.minecraft.world.level.Level.reset(); ServerPlayer p2 = fresh(70); warmup(p2, 70); int m2 = mark();
  for (int r = 0; r < 3; r++) {
   p2.hurtTime = 10; p2.lastDamage = new DamageSource(new LivingEntity(), null, "mob");
   Movement.tick(p2); // arms kbAt
   adv(500); p2.hurtTime = 0; Movement.tick(p2); // evaluates: open space, no movement
  }
  ck(since("antiknockback", m2), "S2: three no-push rounds must signal antiknockback");

  // S3 nofall window (U1): 3-of-5 unsupported ground claims flag; 2-of-5 do not.
  net.minecraft.world.level.Level.reset(); ServerPlayer p3 = fresh(70); warmup(p3, 70);
  net.minecraft.world.level.Level.support = false; int m3 = mark();
  send(p3, 0, 70, 0, true); send(p3, 0, 70, 0, true); ck(!since("nofall", m3), "S3a: 2 unsupported claims must not flag");
  send(p3, 0, 70, 0, true); ck(since("nofall", m3), "S3a: 3-of-5 unsupported claims must flag nofall");
  net.minecraft.world.level.Level.reset(); ServerPlayer p3b = fresh(70); warmup(p3b, 70);
  net.minecraft.world.level.Level.support = false; int m3b = mark();
  send(p3b, 0, 70, 0, true); send(p3b, 0, 70, 0, true);
  net.minecraft.world.level.Level.support = true;
  send(p3b, 0, 70, 0, true); send(p3b, 0, 70, 0, true); send(p3b, 0, 70, 0, true);
  ck(!since("nofall", m3b), "S3b: 2-of-5 unsupported claims must NOT flag nofall");

  // S4 powder band (U2): struggle dy 0.63 near powder is silent (vanilla auto-step + powder band);
  // dy 0.75 near powder is outside every exemption and flags (step).
  net.minecraft.world.level.Level.reset(); ServerPlayer p4 = fresh(70); warmup(p4, 70);
  net.minecraft.world.level.Level.powderFeet = true; net.minecraft.world.level.Level.scriptPowderBody = true; int m4 = mark();
  send(p4, 0, 70.63, 0, true); ck(!since("SIGNAL", m4), "S4a: powder struggle dy 0.63 must produce NO signal");
  net.minecraft.world.level.Level.reset(); ServerPlayer p4b = fresh(70); warmup(p4b, 70);
  net.minecraft.world.level.Level.powderFeet = true; int m4b = mark();
  send(p4b, 0, 70.75, 0, true); ck(since("step dy 0.750", m4b), "S4b: dy 0.75 near powder must flag (step dy 0.750 onGround)");

  // S5 classify (U4): entry gate + eviction resistance. Both modes run the same packet streams.
  net.minecraft.world.level.Level.reset(); ServerPlayer p5 = fresh(70.5); warmup(p5, 70.5);
  net.minecraft.world.level.Level.support = false; net.minecraft.world.level.Level.partialBelow = true; int m5 = mark();
  for (int i = 0; i < 5; i++) send(p5, 0, 70.5, 0, true);
  if (classify) {
   ck(since("[groundtol] tolerated", m5), "S5a(classify): tolerated partial-shape claims must emit the [groundtol] tolerated line");
   ck(!since("nofall", m5), "S5a(classify): 5 tolerated claims must NOT enter the nofall window");
  } else {
   ck(since("nofall", m5), "S5a(plain): without classify the same partial-shape claims are plain unsupported and MUST flag (the U4 status quo)");
  }
  if (classify) {
   net.minecraft.world.level.Level.reset(); ServerPlayer p5b = fresh(70.5); warmup(p5b, 70.5);
   net.minecraft.world.level.Level.support = false; int m5b = mark();
   send(p5b, 0, 70.5, 0, true); send(p5b, 0, 70.5, 0, true); // 2 real unsupported claims
   net.minecraft.world.level.Level.partialBelow = true;
   send(p5b, 0, 70.5, 0, true); send(p5b, 0, 70.5, 0, true); send(p5b, 0, 70.5, 0, true); send(p5b, 0, 70.5, 0, true); // 4 tolerated
   ck(!since("nofall", m5b), "S5b(classify): tolerated claims must not top up the window to 3");
   net.minecraft.world.level.Level.partialBelow = false;
   send(p5b, 0, 70.5, 0, true); // 3rd real unsupported: tolerated claims must NOT have evicted the first two
   ck(since("nofall", m5b), "S5b(classify): the 3rd unsupported claim after tolerated claims MUST flag (eviction resistance)");
  }
  System.out.println("MovementHarness PASS mode=" + (classify ? "classify" : "plain") + " (" + checks + " checks)");
 }
}
'''

class MovementIntegration(unittest.TestCase):
    def run_logged(self, cmd, log, timeout, env_extra=None):
        """Output-capped subprocess, same contract as 0097: RLIMIT_FSIZE=LOG_CAP is a PER-FILE limit on every write
        the child makes (the log AND javac's .class outputs); timeout bounds wall time but kills the direct child
        only (neither javac nor the harness java forks here); only a seek-based bounded tail is read back; detection
        is size-based, so a log at cap size means truncated output and fails the test. preexec_fn is POSIX-only."""
        env = dict(os.environ); env.update(env_extra or {})
        with open(log, 'wb') as lf:
            try:
                r = subprocess.run(cmd, stdout=lf, stderr=subprocess.STDOUT, timeout=timeout, env=env,
                                   preexec_fn=lambda: resource.setrlimit(resource.RLIMIT_FSIZE, (LOG_CAP, LOG_CAP)))
            except subprocess.TimeoutExpired:
                self.fail(f'{cmd[0]} exceeded the {timeout} s bound')
        tail, size = bounded_tail(log)
        print(f'{cmd[0]} log {size} bytes (cap {LOG_CAP}) at {log}')
        if size >= LOG_CAP:
            self.fail(f'{cmd[0]} hit the {LOG_CAP}-byte output cap - log truncated (rc={r.returncode}); cap-brushing output is never a pass')
        return r.returncode, tail

    def test_movement_integration(self):
        gj = gson_jar()
        self.assertTrue(shutil.which('javac'), 'HARD prereq missing: javac not on PATH - this test must RUN, not skip (Box provides the JDK)')
        self.assertIsNotNone(gj, 'HARD prereq missing: no Gson jar (set GSON_JAR or use the gradle cache) - this test must RUN, not skip')
        self.assertTrue(Path(gj).name.startswith('gson-'), f'gson jar sanity: {gj}')
        print('TOOLCHAIN gson jar:', gj, 'sha256', sha(Path(gj).read_bytes()))
        with tempfile.TemporaryDirectory(prefix='movement-it-') as d:
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
                want_ms, want_ns = PIN.get(n, (0, None))
                if want_ms or want_ns:
                    n_ms, n_ns = text.count(CLOCK_MS), text.count(CLOCK_NS)
                    self.assertEqual(n_ms, want_ms, f'expected exactly {want_ms} ms clock sites in production {n} (update the pin on a real change; 0 would mean the rebinding silently no-ops)')
                    text = text.replace(CLOCK_MS, 'dev.meshac.TestClock.ms')
                    self.assertEqual(text.count(CLOCK_MS), 0, f'{n} ms rebinding incomplete')
                    if want_ns is not None:
                        self.assertEqual(n_ns, want_ns, f'expected exactly {want_ns} ns clock sites in production {n} (update the pin on a real change)')
                        text = text.replace(CLOCK_NS, 'dev.meshac.TestClock.ns')
                        self.assertEqual(text.count(CLOCK_NS), 0, f'{n} ns rebinding incomplete')
                    reverted = text.replace('dev.meshac.TestClock.ms', CLOCK_MS).replace('dev.meshac.TestClock.ns', CLOCK_NS)
                    self.assertEqual(reverted, (S/f'{n}.java').read_text(), f'{n} fixture copy differs from production beyond the clock rebinding')
                    print(f'SOURCE {n}.java: clock rebound at {n_ms} ms + {want_ns if want_ns is not None else 0} ns sites (of {n_ns} ns sites), reversal-identical')
                f = root/'prod'/f'dev/meshac/{n}.java'; f.parent.mkdir(parents=True, exist_ok=True); f.write_text(text); sources.append(str(f))
                src_b, fix_b = (S/f'{n}.java').read_bytes(), f.read_bytes()
                manifest.append(f'prod/dev/meshac/{n}.java:source={sha(src_b)}:fixture={sha(fix_b)}')
                if not (want_ms or want_ns):
                    self.assertEqual(src_b, fix_b, f'{n}.java fixture bytes differ from production bytes')
            h = root/'dev/meshac/MovementHarness.java'; h.write_text(HARNESS); sources.append(str(h))
            manifest.append(f'dev/meshac/MovementHarness.java:{sha(HARNESS.encode())}')
            mhash = hashlib.sha256('\n'.join(manifest).encode()).hexdigest()
            print('FIXTURE-MANIFEST sha256', mhash)
            for row in manifest: print('MANIFEST-ROW', row)
            out = root/'out'
            rc, jtail = self.run_logged(['javac','-J-Xmx256m','-cp',gj,'-d',str(out)]+sources, root/'javac.log', 300)
            self.assertEqual(rc, 0, 'javac failed, bounded tail:\n' + jtail)
            rc, htail = self.run_logged(['java','-Xmx256m',f'-Dmeshac.test.configdir={cfgdir}','-cp',f'{out}:{gj}','dev.meshac.MovementHarness','plain'], root/'harness-plain.log', 180)
            self.assertEqual(rc, 0, f'plain harness rc={rc}, bounded tail:\n' + htail)
            self.assertIn('MovementHarness PASS mode=plain', htail)
            rc, ctail = self.run_logged(['java','-Xmx256m',f'-Dmeshac.test.configdir={cfgdir}','-cp',f'{out}:{gj}','dev.meshac.MovementHarness','classify'], root/'harness-classify.log', 180, env_extra={'MESHAC_GROUND_TOL':'classify'})
            self.assertEqual(rc, 0, f'classify harness rc={rc}, bounded tail:\n' + ctail)
            self.assertIn('MovementHarness PASS mode=classify', ctail)
            print(htail.strip()); print(ctail.strip())

if __name__ == '__main__': unittest.main()
