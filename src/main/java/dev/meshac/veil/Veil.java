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
	/** Cache key includes the level: the old key was (chunk x, z, section) only, so the overworld and the nether shared entries and a swap list computed for one dimension was applied to another (wrong "real" state restored into live terrain). */
	private record Key(ServerLevel level, int cx, int cz, int sec) {}
	private static final Map<Key, Mod[]> CACHE = new ConcurrentHashMap<>();
	private static final Mod[] NONE = new Mod[0];
	private static final int CACHE_MAX = 20000;
	private static volatile MinecraftServer srvRef;
	public static void invalidate(ServerLevel l, ChunkPos p) { int cx = p.getMinBlockX() >> 4, cz = p.getMinBlockZ() >> 4; for (int i = 0; i < 32; i++) CACHE.remove(new Key(l, cx, cz, i)); }
	public static void clear() { CACHE.clear(); }
	/** Kill switch: any detected inconsistency turns anti-xray off for the rest of the run and says so once. */
	public static void fail(String why) {
		if (xrayOn) { xrayOn = false; dev.meshac.Meshac.LOG.error("[meshac] chunkveil anti-xray DISABLED: {}", why); }
		CACHE.clear();
	}
	/** Chunk packets are only veiled on the server thread. The swap mutates live sections, so another thread would race the game. */
	public static boolean onServerThread() { MinecraftServer s = srvRef; return s != null && s.isSameThread(); }

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
		if (!xrayOn || !(c.getLevel() instanceof ServerLevel lvl) || !onServerThread()) return null;
		if (sec.hasOnlyAir() || !sec.maybeHas(Veil::hidden)) return null; // VERIFY maybeHas(Predicate<BlockState>)
		if (CACHE.size() > CACHE_MAX) CACHE.clear();
		return CACHE.computeIfAbsent(new Key(lvl, c.getPos().getMinBlockX() >> 4, c.getPos().getMinBlockZ() >> 4, secIdx), k -> {
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
			return out.isEmpty() ? NONE : out.toArray(new Mod[0]);
		});
	}
	private static boolean isStone(BlockState s) { return s.is(Blocks.STONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.NETHERRACK) || s.is(Blocks.END_STONE); }


	/**
	 * Build the version of a section that goes on the wire: a private COPY of the live section with the planned positions swapped to their fakes.
	 * The live section is never written, so there is nothing to restore, no race with the game thread, and a bug here can only change what one packet shows.
	 * Returns null when nothing was applied or the copy failed (caller then sends the real section: fail open).
	 * Entries whose current state no longer equals the cached "real" state are stale: skipped, and the cache is dropped.
	 */
	public static net.minecraft.world.level.chunk.LevelChunkSection veiled(net.minecraft.world.level.chunk.LevelChunkSection sec, Mod[] m) {
		try {
			net.minecraft.world.level.chunk.LevelChunkSection copy = sec.copy(); // public on all 4 versions (checked in the server jars)
			int applied = 0; boolean stale = false;
			for (Mod x : m) {
				int lx = x.idx() & 15, ly = (x.idx() >> 4) & 15, lz = x.idx() >> 8;
				if (copy.getBlockState(lx, ly, lz) != x.real()) { stale = true; continue; }
				copy.setBlockState(lx, ly, lz, x.fake(), false); // false = no lock; fine, the copy is private to this thread
				applied++;
			}
			if (stale) CACHE.clear();
			return applied == 0 ? null : copy;
		} catch (Throwable t) {
			dev.meshac.Meshac.LOG.warn("[meshac] chunkveil could not build a veiled section, sending it unveiled: {}", t.toString());
			return null;
		}
	}

	/** Block at (x,y,z) just changed to a see-through state: re-send now-exposed hidden neighbours to everyone tracking the chunk. */
	public static void reveal(ServerLevel l, BlockPos pos, BlockState now) {
		if (!xrayOn) return;
		invalidate(l, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
		if (!transparent(now)) return;
		for (int[] o : XrayCore.NEIGHBOURS) {
			BlockPos n = pos.offset(o[0], o[1], o[2]);
			// neighbour may be in an adjacent chunk whose cached mods also changed
			invalidate(l, new ChunkPos(n.getX() >> 4, n.getZ() >> 4));
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

	/**
	 * Foliage-aware ray. Vanilla ClipContext.Block.VISUAL stops on leaves (their visual shape is a full cube), so a player standing behind a bush was culled although a real client sees
	 * him through the gaps and the sprite cut-outs. This walks the voxels itself and only an OCCLUDING block with a visual shape the ray really crosses blocks it. Leaves, glass, ice,
	 * slime, vines, fences, doors, scaffolding and cobwebs (anything where canOcclude() is false) let the ray through. The cost is that a tree line no longer hides players from a wallhack;
	 * that is the point: a missed cull is invisible, a wrong cull is a vanished player. Walls, stone, dirt, wood and the like still block as before.
	 * UNCOMPILED. VERIFY: BlockGetter.traverseBlocks(Vec3, Vec3, T, BiFunction, Function) and VoxelShape.clip(Vec3, Vec3, BlockPos) exist under those names in 26.x.
	 */
	private static boolean rayClear(ServerLevel l, Vec3 from, Vec3 to) {
		Boolean blocked = net.minecraft.world.level.BlockGetter.traverseBlocks(from, to, l, (lv, pos) -> {
			BlockState st = lv.getBlockState(pos);
			if (st.isAir() || !st.canOcclude() || st.is(net.minecraft.tags.BlockTags.LEAVES)) return null;
			var shape = st.getVisualShape(lv, pos, net.minecraft.world.phys.shapes.CollisionContext.empty()); // VERIFY signature
			if (shape.isEmpty()) return null;
			return shape.clip(from, to, pos) != null ? Boolean.TRUE : null;
		}, lv -> Boolean.FALSE);
		return !Boolean.TRUE.equals(blocked);
	}

	/** Multi-point LOS: visible if any sample point of the target is reachable from the eye (or from the eye shifted by the viewer's velocity over 6 ticks). */
	private static boolean visible(ServerLevel l, ServerPlayer v, Entity e) {
		AABB b = e.getBoundingBox();
		double[] pts = EspCore.samplePoints(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
		Vec3 eye = v.getEyePosition(), vel = v.getDeltaMovement();
		Vec3[] eyes = { eye, eye.add(vel.x * 6, 0, vel.z * 6), eye.add(0, -0.4, 0) }; // third origin: a crouch or a step down changes the eye by about this much
		for (Vec3 o : eyes) for (int i = 0; i < pts.length; i += 3) {
			if (rayClear(l, o, new Vec3(pts[i], pts[i + 1], pts[i + 2]))) return true;
		}
		return false;
	}

	private static int rr = 0;
	/** Called once per server tick (END_SERVER_TICK). Every pair is checked once per TICK_PERIOD, budgeted. */
	public static void tick(MinecraftServer srv) {
		srvRef = srv;
		if (!espOn) return;
		long tick = srv.getTickCount();
		int budget = RAYS_PER_TICK;
		for (ServerLevel l : srv.getAllLevels()) {
			var cm = l.getChunkSource().chunkMap;
			for (ServerPlayer v : l.players()) {
				if ((v.getId() + tick) % TICK_PERIOD != 0) continue; // stagger players across ticks
				for (Entity e : l.getAllEntities()) { // VERIFY cheaper: iterate the tracked set via an accessor on ChunkMap.entityMap
					if (!cullable(e) || e == v || e.level() != l) continue;
					boolean wasHidden = ESP.hidden(v.getId(), e.getId());
					if (budget <= 0 && !wasHidden) continue; // out of budget: skip NEW culls only. The old "return" left already hidden entities hidden (and starved every later viewer) until the next tick had budget, which under load meant until relog.
					double d = Math.sqrt(e.getBoundingBox().distanceToSqr(v.getEyePosition()));
					if (d > 128) continue;
					boolean ex = exempt(v, e, tick);
					boolean vis = ex || d <= 8 || visible(l, v, e);
					if (!ex && d > 8 && !wasHidden) budget--; // re-show checks of hidden pairs are free: they must never be skipped
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
