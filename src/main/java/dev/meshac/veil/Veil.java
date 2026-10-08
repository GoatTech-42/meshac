package dev.meshac.veil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Minecraft glue for chunkveil (anti-xray) and the entity cull (anti-ESP). Logic lives in XrayCore and EspCore.
 * UNCOMPILED: written against the 26.1.2 Mojang names the server agent listed. Lines marked VERIFY use a method not on that list.
 */
public final class Veil {
	// TODO wire to Config.veil (presets). Defaults per Luke: fake ores on, mobs culled.
	public static volatile boolean xrayOn = true, espOn = true, cullMobs = true;
	public static volatile double fakeRatio = 0.02;
	public static final int TICK_PERIOD = 4, RAYS_PER_TICK = 200, JOIN_GRACE_TICKS = 40;

	// ---------- xray ----------
	private static final Map<Block, Boolean> HIDDEN = new ConcurrentHashMap<>();
	public static boolean hidden(BlockState s) {
		return HIDDEN.computeIfAbsent(s.getBlock(), b -> {
			String p = BuiltInRegistries.BLOCK.getKey(b).getPath();
			return p.endsWith("_ore") || p.equals("ancient_debris") || p.equals("chest") || p.equals("trapped_chest") || p.equals("ender_chest") || p.equals("spawner");
		});
	}
	/** Transparent for exposure purposes: anything a player could see through or into. Conservative (fails open). */
	public static boolean transparent(BlockState s) { return !s.canOcclude() || !s.getFluidState().isEmpty(); } // VERIFY canOcclude() exists on BlockState (it did through 1.21)

	/** View over a chunk plus its loaded neighbours. Unloaded neighbour = transparent (fail open). */
	public static final class View implements XrayCore.Blocks {
		final LevelChunk c; final ServerLevel lvl; final BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
		public View(LevelChunk c) { this.c = c; this.lvl = c.getLevel() instanceof ServerLevel sl ? sl : null; }
		BlockState at(int x, int y, int z) {
			if (y < c.getMinY() || y >= c.getMaxY()) return null; // VERIFY getMinY/getMaxY (older: getMinBuildHeight/getMaxBuildHeight)
			LevelChunk t = c;
			int cx = x >> 4, cz = z >> 4;
			if (cx != (c.getPos().getMinBlockX() >> 4) || cz != (c.getPos().getMinBlockZ() >> 4)) {
				if (lvl == null) return null;
				t = lvl.getChunkSource().getChunkNow(cx, cz);
				if (t == null) return null;
			}
			return t.getBlockState(mp.set(x, y, z));
		}
		public boolean hidden(int x, int y, int z) { BlockState s = at(x, y, z); return s != null && Veil.hidden(s); }
		public boolean transparent(int x, int y, int z) {
			if (y < c.getMinY() || y >= c.getMaxY()) return false; // world top/bottom: nothing to see through
			BlockState s = at(x, y, z); return s == null || Veil.transparent(s);
		}
	}

	/** One modification: local index (x | y<<4 | z<<8) and the state to write while encoding. */
	public record Mod(int idx, BlockState fake, BlockState real) {}
	private static final Map<Long, Mod[]> CACHE = new ConcurrentHashMap<>();
	private static long ck(ChunkPos p, int sec) { return ((((long) (p.getMinBlockX() >> 4)) << 32 | ((p.getMinBlockZ() >> 4) & 0xffffffffL)) << 5) ^ (sec & 31); }
	public static void invalidate(ChunkPos p) { for (int i = 0; i < 32; i++) CACHE.remove(ck(p, i)); }
	public static void clear() { CACHE.clear(); }

	private static BlockState stoneFor(ServerLevel l, int y) {
		if (l.dimension() == Level.NETHER) return Blocks.NETHERRACK.defaultBlockState();
		if (l.dimension() == Level.END) return Blocks.END_STONE.defaultBlockState();
		return y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
	}
	private static BlockState fakeOre(ServerLevel l, int y, long salt, int x, int z) {
		if (l.dimension() == Level.NETHER) return Blocks.NETHER_QUARTZ_ORE.defaultBlockState();
		if (l.dimension() == Level.END) return null;
		long h = salt ^ (x * 31L + z * 17L + y);
		return switch ((int) Math.floorMod(h * 0x9E3779B97F4A7C15L >>> 33, 4L)) {
			case 0 -> y < 0 ? Blocks.DEEPSLATE_COAL_ORE.defaultBlockState() : Blocks.COAL_ORE.defaultBlockState();
			case 1 -> y < 0 ? Blocks.DEEPSLATE_IRON_ORE.defaultBlockState() : Blocks.IRON_ORE.defaultBlockState();
			case 2 -> y < 0 ? Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState() : Blocks.GOLD_ORE.defaultBlockState();
			default -> y < 0 ? Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState() : Blocks.IRON_ORE.defaultBlockState();
		};
	}

	/** Compute (cached) modifications for one section. secIdx = index in chunk.getSections(). */
	public static Mod[] mods(LevelChunk c, int secIdx, net.minecraft.world.level.chunk.LevelChunkSection sec) {
		if (!xrayOn || !(c.getLevel() instanceof ServerLevel lvl)) return null;
		if (sec.hasOnlyAir() || !sec.maybeHas(Veil::hidden)) return null; // VERIFY maybeHas(Predicate<BlockState>)
		return CACHE.computeIfAbsent(ck(c.getPos(), secIdx), k -> {
			View v = new View(c);
			int baseY = (c.getMinSectionY() + secIdx) << 4, bx = c.getPos().getMinBlockX(), bz = c.getPos().getMinBlockZ(); // VERIFY getMinSectionY
			long salt = lvl.getSeed() * 0x2545F4914F6CDD1DL; // VERIFY getSeed on ServerLevel
			List<Mod> out = new ArrayList<>();
			for (int ly = 0; ly < 16; ly++) for (int lz = 0; lz < 16; lz++) for (int lx = 0; lx < 16; lx++) {
				BlockState s = sec.getBlockState(lx, ly, lz);
				int x = bx + lx, y = baseY + ly, z = bz + lz, idx = lx | ly << 4 | lz << 8;
				if (Veil.hidden(s)) {
					if (XrayCore.shouldHide(v, x, y, z)) out.add(new Mod(idx, stoneFor(lvl, y), s));
				} else if (fakeRatio > 0 && isStone(s) && XrayCore.fakeOreAt(salt, x, y, z, fakeRatio) && !XrayCore.exposed(v, x, y, z)) {
					BlockState f = fakeOre(lvl, y, salt, x, z);
					if (f != null) out.add(new Mod(idx, f, s));
				}
			}
			return out.toArray(new Mod[0]);
		});
	}
	private static boolean isStone(BlockState s) { return s.is(Blocks.STONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.NETHERRACK) || s.is(Blocks.END_STONE); }

	/** Block at (x,y,z) just changed to a see-through state: re-send now-exposed hidden neighbours to everyone tracking the chunk. */
	public static void reveal(ServerLevel l, BlockPos pos, BlockState now) {
		if (!xrayOn) return;
		invalidate(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
		if (!transparent(now)) return;
		for (int[] o : XrayCore.NEIGHBOURS) {
			BlockPos n = pos.offset(o[0], o[1], o[2]);
			// neighbour may be in an adjacent chunk whose cached mods also changed
			invalidate(new ChunkPos(n.getX() >> 4, n.getZ() >> 4));
			BlockState s = l.getBlockState(n);
			if (!hidden(s)) continue;
			var pk = new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(n, s);
			for (ServerPlayer p : l.getChunkSource().chunkMap.getPlayers(new ChunkPos(n.getX() >> 4, n.getZ() >> 4), false)) p.connection.send(pk); // VERIFY chunkMap public + getPlayers(ChunkPos,boolean)
			// chest/spawner block entity data: let vanilla send the BE update packet
			var be = l.getBlockEntity(n);
			if (be != null) { var bp = be.getUpdatePacket(); if (bp != null) for (ServerPlayer p : l.getChunkSource().chunkMap.getPlayers(new ChunkPos(n.getX() >> 4, n.getZ() >> 4), false)) p.connection.send(bp); }
		}
	}

	// ---------- esp ----------
	private static final EspCore ESP = new EspCore(new EspCore.Params());
	private static final Map<Integer, Long> JOIN = new ConcurrentHashMap<>(); // player id -> tick of join/teleport/dimension change
	public static void grace(ServerPlayer p, long tick) { JOIN.put(p.getId(), tick); }
	public static void attacked(ServerPlayer p, Entity e, long tick) { ESP.attacked(p.getId(), e.getId(), tick); }
	public static boolean isHidden(ServerPlayer p, Entity e) { return espOn && ESP.hidden(p.getId(), e.getId()); }
	public static void forgetPlayer(ServerPlayer p) { JOIN.remove(p.getId()); }

	private static boolean exempt(ServerPlayer v, Entity e, long tick) {
		if (e == v || e.getVehicle() == v || v.getVehicle() == e || e.hasPassenger(v) || v.hasPassenger(e)) return true; // VERIFY hasPassenger(Entity)
		if (e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss) return true;
		if (e.isCurrentlyGlowing() || e.isInvisible() && false) return true; // VERIFY isCurrentlyGlowing
		if (e instanceof net.minecraft.world.entity.projectile.Projectile) return true;
		if (e instanceof net.minecraft.world.entity.Leashable l && l.getLeashHolder() == v) return true; // VERIFY Leashable (1.21.2+; absent in older, 1.21.11 has it)
		if (v.isSpectator() || v.isCreative()) return true;
		if (v.getTeam() != null && v.getTeam() == e.getTeam()) return true;
		Long j = JOIN.get(v.getId()); if (j != null && tick - j < JOIN_GRACE_TICKS) return true;
		if (e instanceof ServerPlayer) return false;
		return !cullMobs; // non player: cull only when mobs are enabled; item frames, armor stands, items, vehicles: never culled
	}
	private static boolean cullable(Entity e) {
		if (e instanceof ServerPlayer) return true;
		return e instanceof net.minecraft.world.entity.Mob;
	}

	/** Multi-point LOS: visible if any sample point of the target is reachable from the eye (or from the eye shifted by the viewer's velocity over 6 ticks). */
	private static boolean visible(ServerLevel l, ServerPlayer v, Entity e) {
		AABB b = e.getBoundingBox();
		double[] pts = EspCore.samplePoints(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
		Vec3 eye = v.getEyePosition(), vel = v.getDeltaMovement();
		Vec3[] eyes = { eye, eye.add(vel.x * 6, 0, vel.z * 6) };
		for (Vec3 o : eyes) for (int i = 0; i < pts.length; i += 3) {
			Vec3 t = new Vec3(pts[i], pts[i + 1], pts[i + 2]);
			var r = l.clip(new net.minecraft.world.level.ClipContext(o, t, net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, v)); // VISUAL: glass, leaves, fences do not block like solid ones; VERIFY it is enough for leaves/glass (see test E5)
			if (r.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return true;
		}
		return false;
	}

	private static int rr = 0;
	/** Called once per server tick (END_SERVER_TICK). Every pair is checked once per TICK_PERIOD, budgeted. */
	public static void tick(MinecraftServer srv) {
		if (!espOn) return;
		long tick = srv.getTickCount();
		int budget = RAYS_PER_TICK;
		for (ServerLevel l : srv.getAllLevels()) {
			var cm = l.getChunkSource().chunkMap;
			for (ServerPlayer v : l.players()) {
				if ((v.getId() + tick) % TICK_PERIOD != 0) continue; // stagger players across ticks
				for (Entity e : l.getAllEntities()) { // VERIFY cheaper: iterate the tracked set via an accessor on ChunkMap.entityMap
					if (budget <= 0) return;
					if (!cullable(e) || e == v || e.level() != l) continue;
					double d = Math.sqrt(e.getBoundingBox().distanceToSqr(v.getEyePosition()));
					if (d > 128) continue;
					boolean ex = exempt(v, e, tick);
					boolean vis = ex || d <= 8 || visible(l, v, e);
					if (!ex && d > 8) budget--;
					var a = ESP.update(v.getId(), e.getId(), tick, d, ex, vis);
					if (a == EspCore.Action.HIDE) hide(cm, v, e);
					else if (a == EspCore.Action.SHOW) show(cm, v, e);
				}
			}
		}
	}
	// TrackedEntity access goes through TrackedEntityAccess (mixin accessor to ChunkMap.entityMap).
	private static dev.meshac.veil.TrackedEntityAccess tracked(Object chunkMap, int id) { return (dev.meshac.veil.TrackedEntityAccess) ((dev.meshac.veil.ChunkMapAccess) chunkMap).meshac$entityMap().get(id); }
	private static void hide(Object cm, ServerPlayer v, Entity e) { var t = tracked(cm, e.getId()); if (t != null) t.meshac$remove(v); }
	private static void show(Object cm, ServerPlayer v, Entity e) { var t = tracked(cm, e.getId()); if (t != null) t.meshac$update(v); }
}
