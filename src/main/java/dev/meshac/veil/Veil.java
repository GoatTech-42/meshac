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
 * Compiles on all four version targets (Box-REPORTED 10-17 11:36pm, series through 0066 - HISTORICAL:
 * changes to this file since are comment-only, but the current-tip binary is NOT verified): the Mojang names
 * the old UNCOMPILED banner worried about resolve everywhere that build covered. Compile proves names, not
 * behaviour - runtime evidence is rig-only (docs/veil/RIG-QUICKSTART.md), never proven with a real client yet.
 */
public final class Veil {
	// Config wiring lives in Config.applyVeil (startup and /mesh reload): veilEnabled && veilXray/veilEsp/veilBedrock
	// (worker-grounded: Config.java:110 + :27-30, not re-derived in this patch's review context).
	// The field initializers below are pre-config defaults that applyVeil OVERWRITES at startup, so out of the box
	// the veils are OFF (config keys default unset) until they are proven on a real client. Veil switches are
	// independent of the punishment presets by design; a preset-driven veil default would be an owner decision.
	public static volatile boolean bedrockOn = false; // veilBedrock: false = Bedrock viewers are never culled
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
	public static boolean transparent(BlockState s) { return !s.canOcclude() || !s.getFluidState().isEmpty(); } // canOcclude() compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)

	/** View over a chunk plus its loaded neighbours. Unloaded neighbour = transparent (fail open). */
	public static final class View implements XrayCore.Blocks {
		final LevelChunk c; final ServerLevel lvl; final BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
		public View(LevelChunk c) { this.c = c; this.lvl = c.getLevel() instanceof ServerLevel sl ? sl : null; }
		BlockState at(int x, int y, int z) {
			if (y < c.getMinY() || y >= c.getMaxY()) return null; // getMinY/getMaxY compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)
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
	/** A cached plan: the modifications, the neighbour-loaded mask at plan time (leak 2) and, for planned blocks on the chunk border, their positions and outward see-through bits (leak 1). See docs/veil/BORDER-LEAKS.md. */
	private record Plan(Mod[] mods, int mask, int[] borderIdx, int[] borderBits) {}
	private static final Map<Key, Plan> CACHE = new ConcurrentHashMap<>();
	private static final Mod[] NONE = new Mod[0];
	private static final int[] NO_INTS = new int[0];
	/** Sections rebuilt because a side neighbour chunk loaded after the plan, or because a planned border block's outward neighbour changed. Shown by /mesh veil. */
	public static volatile long neighbourRebuilds = 0, borderRebuilds = 0;
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
		Plan p = modsCached(c, secIdx, sec);
		if (p == null) return null;
		ServerLevel lvl = lvlOf(c);
		int why = PlanGuard.check(() -> p.mods().length == 0 || fresh(sec, p.mods()), p.mask(), () -> maskOf(lvl, c), () -> borderUnchanged(c, secIdx, p));
		if (why == PlanGuard.OK) return p.mods();
		// Cached plan no longer matches: a changed block in this section (no-update setblock, piston, other mod, worldedit), a side chunk that loaded after the plan, or a changed block across the border. Drop ONLY this section and rebuild once.
		// veiled() still skips any entry that is stale at copy time, so a race after this check fails open. This replaces the CACHE.clear() the stale path used, which threw away every section on every miss.
		if (lvl != null) CACHE.remove(new Key(lvl, c.getPos().getMinBlockX() >> 4, c.getPos().getMinBlockZ() >> 4, secIdx));
		if (why == PlanGuard.OWN_STALE) staleRebuilds++; else if (why == PlanGuard.NEIGHBOUR_LOADED) neighbourRebuilds++; else borderRebuilds++;
		Plan q = modsCached(c, secIdx, sec);
		return q == null ? null : q.mods();
	}
	/** Which of the four side neighbours of this chunk are loaded right now. */
	private static int maskOf(ServerLevel lvl, LevelChunk c) {
		if (lvl == null) return 15; // cannot look: never trigger a replan
		int cx = c.getPos().getMinBlockX() >> 4, cz = c.getPos().getMinBlockZ() >> 4; var src = lvl.getChunkSource();
		return NeighbourMask.of(src.getChunkNow(cx - 1, cz) != null, src.getChunkNow(cx + 1, cz) != null, src.getChunkNow(cx, cz - 1) != null, src.getChunkNow(cx, cz + 1) != null);
	}
	private static boolean borderUnchanged(LevelChunk c, int secIdx, Plan p) {
		if (p.borderIdx().length == 0) return true;
		View v = new View(c); int bx = c.getPos().getMinBlockX(), bz = c.getPos().getMinBlockZ(), by = (c.getMinSectionY() + secIdx) << 4;
		return BorderGuard.allUnchanged(p.borderIdx(), p.borderBits(), (i, dx, dz) -> { int idx = p.borderIdx()[i]; return v.transparent(bx + (idx & 15) + dx, by + ((idx >> 4) & 15), bz + ((idx >> 8) & 15) + dz); });
	}

	/** Count of sections rebuilt because the cache was stale. Shown by /mesh veil; a steady climb means an invalidate() call is missing somewhere. */
	public static volatile long staleRebuilds = 0;
	/** Block entity entries dropped from chunk packets (hidden chests, spawners), and chunks where the list and the walk differed in size so nothing was dropped. Shown by /mesh veil: the rig reads these to see the strip working. */
	public static volatile long beStripped = 0, beSizeMismatch = 0;
	private static ServerLevel lvlOf(LevelChunk c) { return c.getLevel() instanceof ServerLevel l ? l : null; }
	/** True when every planned position still holds the state the plan was computed from. */
	private static boolean fresh(net.minecraft.world.level.chunk.LevelChunkSection sec, Mod[] m) {
		return PlanFresh.fresh(m.length, i -> m[i].idx(), i -> m[i].real(), p -> sec.getBlockState(p & 15, (p >> 4) & 15, p >> 8));
	}
	private static Plan modsCached(LevelChunk c, int secIdx, net.minecraft.world.level.chunk.LevelChunkSection sec) {
		if (!dev.meshac.Config.get().veilEnabled() || !xrayOn || !(c.getLevel() instanceof ServerLevel lvl) || !onServerThread()) return null;
		if (sec.hasOnlyAir() || !sec.maybeHas(Veil::hidden)) return null; // maybeHas(Predicate<BlockState>) compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)
		if (CACHE.size() > CACHE_MAX) CACHE.clear();
		return CACHE.computeIfAbsent(new Key(lvl, c.getPos().getMinBlockX() >> 4, c.getPos().getMinBlockZ() >> 4, secIdx), k -> {
			View v = new View(c);
			int baseY = (c.getMinSectionY() + secIdx) << 4, bx = c.getPos().getMinBlockX(), bz = c.getPos().getMinBlockZ(); // getMinSectionY compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)
			long salt = salt(lvl);
			List<Mod> out = new ArrayList<>();
			for (int ly = 0; ly < 16; ly++) for (int lz = 0; lz < 16; lz++) for (int lx = 0; lx < 16; lx++) {
				BlockState s = sec.getBlockState(lx, ly, lz);
				int x = bx + lx, y = baseY + ly, z = bz + lz, idx = lx | ly << 4 | lz << 8;
				if (Veil.hidden(s)) {
					if (XrayCore.shouldHide(v, x, y, z)) {
						BlockState shown = stoneFor(lvl, y);
						BlockPos hp = new BlockPos(x, y, z);
						// a block far harder than what the client would be shown could read as an early stop in the mining check (see HideRule): leave it as it is
						if (HideRule.safeToMask(s.getDestroySpeed(lvl, hp), shown.getDestroySpeed(lvl, hp))) out.add(new Mod(idx, shown, s));
					}
				} else if (fakeRatio > 0 && isStone(s) && XrayCore.fakeOreAt(salt, x, y, z, fakeRatio) && !XrayCore.exposed(v, x, y, z)) {
					BlockState f = fakeOre(lvl, y, salt, x, z);
					if (f != null) out.add(new Mod(idx, f, s));
				}
			}
			Mod[] mods = out.isEmpty() ? NONE : out.toArray(new Mod[0]);
			java.util.ArrayList<Integer> bi = new java.util.ArrayList<>(); for (Mod m : mods) if (BorderGuard.isBorder(m.idx())) bi.add(m.idx());
			int[] borderIdx = bi.isEmpty() ? NO_INTS : bi.stream().mapToInt(Integer::intValue).toArray();
			int[] borderBits = borderIdx.length == 0 ? NO_INTS : BorderGuard.compute(borderIdx, (i, dx, dz) -> { int idx = borderIdx[i]; return v.transparent(bx + (idx & 15) + dx, baseY + ((idx >> 4) & 15), bz + ((idx >> 8) & 15) + dz); });
			return new Plan(mods, maskOf(lvl, c), borderIdx, borderBits);
		});
	}
	private static long salt(ServerLevel lvl) { return lvl.getSeed() * 0x2545F4914F6CDD1DL; } // getSeed on ServerLevel compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)
	private static boolean isStone(BlockState s) { return s.is(Blocks.STONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.NETHERRACK) || s.is(Blocks.END_STONE); }


	/**
	 * Build the version of a section that goes on the wire: a private COPY of the live section with the planned positions swapped to their fakes.
	 * The live section is never written, so there is nothing to restore, no race with the game thread, and a bug here can only change what one packet shows.
	 * Returns null when nothing was applied or the copy failed (caller then sends the real section: fail open).
	 * Entries whose current state no longer equals the cached "real" state are stale: skipped, and the cache is dropped.
	 */
	public static net.minecraft.world.level.chunk.LevelChunkSection veiled(net.minecraft.world.level.chunk.LevelChunkSection sec, Mod[] m) {
  VeiledSection result = veiledResult(sec, m);
  return result == null ? null : result.section();
 }
 /** Exact successful copy and block-entity positions actually replaced in that copy. Null on unveiled fallback. */
 public record VeiledSection(net.minecraft.world.level.chunk.LevelChunkSection section, int[] maskedEntities) {}
 public static VeiledSection veiledResult(net.minecraft.world.level.chunk.LevelChunkSection sec, Mod[] m) {
		try {
			net.minecraft.world.level.chunk.LevelChunkSection copy = sec.copy(); // public on all 4 versions (checked in the server jars)
			int applied = 0, entityCount = 0; int[] entities = new int[m.length]; boolean stale = false;
			for (Mod x : m) {
				int lx = x.idx() & 15, ly = (x.idx() >> 4) & 15, lz = x.idx() >> 8;
				if (copy.getBlockState(lx, ly, lz) != x.real()) { stale = true; continue; }
				copy.setBlockState(lx, ly, lz, x.fake(), false); // false = no lock; fine, the copy is private to this thread
    if (x.real().hasBlockEntity() && x.fake() != x.real()) entities[entityCount++] = x.idx();
				applied++;
			}
			if (stale) staleRebuilds++; // mods() already revalidates; reaching this means a change landed between the plan and the copy. Skip the entry (done above), keep the cache, next packet rebuilds.
			return applied == 0 ? null : new VeiledSection(copy, java.util.Arrays.copyOf(entities, entityCount));
		} catch (Throwable t) {
			dev.meshac.Meshac.LOG.warn("[meshac] chunkveil could not build a veiled section, sending it unveiled: {}", t.toString());
			return null;
		}
	}

	/** Block at (x,y,z) just changed to a see-through state: re-send now-exposed hidden neighbours to everyone tracking the chunk. */
	public static void reveal(ServerLevel l, BlockPos pos, BlockState old, BlockState now) {
		if (!xrayOn) return;
		if (!ChangeFilter.affectsPlan(hidden(old), hidden(now), transparent(old), transparent(now), isStone(old), isStone(now))) return; // crops, doors, lamps, water: nothing the plan depends on changed
		invalidate(l, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
		if (!transparent(now)) return;
		for (int[] o : XrayCore.NEIGHBOURS) {
			BlockPos n = pos.offset(o[0], o[1], o[2]);
			// neighbour may be in an adjacent chunk whose cached mods also changed
			invalidate(l, new ChunkPos(n.getX() >> 4, n.getZ() >> 4));
			BlockState s = l.getBlockState(n);
			if (!XrayCore.resendOnReveal(hidden(s), isStone(s), salt(l), n.getX(), n.getY(), n.getZ(), fakeRatio)) continue;
			var pk = new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(n, s);
			for (ServerPlayer p : l.getChunkSource().chunkMap.getPlayers(new ChunkPos(n.getX() >> 4, n.getZ() >> 4), false)) p.connection.send(pk); // chunkMap field + getPlayers(ChunkPos,boolean) compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)
			// chest/spawner block entity data: let vanilla send the BE update packet
			var be = l.getBlockEntity(n);
			if (be != null) { var bp = be.getUpdatePacket(); if (bp != null) for (ServerPlayer p : l.getChunkSource().chunkMap.getPlayers(new ChunkPos(n.getX() >> 4, n.getZ() >> 4), false)) p.connection.send(bp); }
		}
	}

	// ---------- esp ----------
	private static final EspCore ESP = new EspCore(new EspCore.Params());
	private static final ViewerEpoch EPOCH = new ViewerEpoch(); // notices joins, respawns (new entity id) and dimension changes from the tick loop, no Fabric event needed
	private static final GraceTable GRACE = new GraceTable(); // player id -> tick of join, respawn or dimension change
	public static void grace(ServerPlayer p, long tick) { GRACE.start(p.getId(), tick); }
	public static void attacked(ServerPlayer p, Entity e, long tick) { ESP.attacked(p.getId(), e.getId(), tick); }
	public static boolean isHidden(ServerPlayer p, Entity e) { return dev.meshac.Config.get().veilEnabled() && espOn && ESP.hidden(p.getId(), e.getId()); }
	public static void forgetPlayer(ServerPlayer p) { GRACE.forget(p.getId()); EPOCH.forget(p.getId()); ESP.forgetViewer(p.getId()); }

	private static boolean exempt(ServerPlayer v, Entity e, long tick) {
		ExemptRule.F f = new ExemptRule.F();
		f.self = e == v;
		f.mountedTogether = e.getVehicle() == v || v.getVehicle() == e || e.hasPassenger(v) || v.hasPassenger(e); // hasPassenger(Entity) compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)
		f.sharesVehicle = e.getVehicle() != null && e.getVehicle() == v.getVehicle(); // two riders of one boat or horse
		f.boss = e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss;
		f.glowing = e.isCurrentlyGlowing(); // isCurrentlyGlowing compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)
		f.projectile = e instanceof net.minecraft.world.entity.projectile.Projectile;
		f.leashedToViewer = e instanceof net.minecraft.world.entity.Leashable l && l.getLeashHolder() == v; // Leashable compile-verified on all 4 targets (Box-reported 10-17 11:36pm, through 0066; comment-only changes since)
		f.viewerSpectatorOrCreative = v.isSpectator() || v.isCreative();
		f.bedrockShowAll = ViewerPolicy.showAll(dev.meshac.BedrockIds.DEFAULT.isBedrock(v.getUUID()), bedrockOn); // zero-high UUID, or Floodgate says so (linked accounts)
		f.sameTeam = v.getTeam() != null && v.getTeam() == e.getTeam();
		f.joinGrace = GRACE.active(v.getId(), tick, JOIN_GRACE_TICKS);
		f.targetIsPlayer = e instanceof ServerPlayer;
		f.cullMobs = cullMobs;
		return ExemptRule.exempt(f);
	}
	private static boolean cullable(Entity e) {
		if (e instanceof ServerPlayer) return true;
		return e instanceof net.minecraft.world.entity.Mob;
	}

	/**
	 * Foliage-aware ray. Vanilla ClipContext.Block.VISUAL stops on leaves (their visual shape is a full cube), so a player standing behind a bush was culled although a real client sees
	 * him through the gaps and the sprite cut-outs. This walks the voxels itself and only an OCCLUDING block with a visual shape the ray really crosses blocks it. Leaves, glass, ice,
	 * slime, vines, doors, scaffolding and cobwebs (anything where canOcclude() is false) and fences, gates and walls (tag-excluded: canOcclude() is true for them) let the ray through. The cost is that a tree line no longer hides players from a wallhack;
	 * that is the point: a missed cull is invisible, a wrong cull is a vanished player. Walls, stone, dirt, wood and the like still block as before.
	 * UNCOMPILED. VERIFY: BlockGetter.traverseBlocks(Vec3, Vec3, T, BiFunction, Function) and VoxelShape.clip(Vec3, Vec3, BlockPos) exist under those names in 26.x.
	 */
	private static boolean rayClear(ServerLevel l, Vec3 from, Vec3 to) {
		Boolean blocked = net.minecraft.world.level.BlockGetter.traverseBlocks(from, to, l, (lv, pos) -> {
			BlockState st = lv.getBlockState(pos);
			// fences, gates, walls: canOcclude() is true and the visual shape is the 1.5-tall collision box, but the model has gaps (see RayBlocking).
			boolean gappy = st.is(net.minecraft.tags.BlockTags.FENCES) || st.is(net.minecraft.tags.BlockTags.FENCE_GATES) || st.is(net.minecraft.tags.BlockTags.WALLS);
			boolean foliage = st.is(net.minecraft.tags.BlockTags.LEAVES);
			if (st.isAir() || !st.canOcclude() || foliage || gappy) return null; // cheap exit before the shape lookup
			var shape = st.getVisualShape(lv, pos, net.minecraft.world.phys.shapes.CollisionContext.empty()); // VERIFY signature
			boolean crosses = !shape.isEmpty() && shape.clip(from, to, pos) != null;
			return RayBlocking.blocks(false, true, foliage, gappy, shape.isEmpty(), crosses) ? Boolean.TRUE : null;
		}, lv -> Boolean.FALSE);
		return !Boolean.TRUE.equals(blocked);
	}

	/** Multi-point LOS: visible if any sample point of the target is reachable from the eye (or from the eye shifted by the viewer's velocity over 6 ticks). */
	private static final double[] SAMPLE_BUF = new double[EspCore.SAMPLE_DOUBLES];
	private static boolean visible(ServerLevel l, ServerPlayer v, Entity e) {
		AABB b = e.getBoundingBox();
		double[] pts = SAMPLE_BUF; int np = EspCore.samplePointsInto(pts, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ); // server thread only, so one shared buffer is safe
		Vec3 eye = v.getEyePosition(), vel = v.getDeltaMovement();
		Vec3[] eyes = { eye, eye.add(vel.x * 6, 0, vel.z * 6), eye.add(0, -0.4, 0) }; // third origin: a crouch or a step down changes the eye by about this much
		for (Vec3 o : eyes) for (int i = 0; i < np; i += 3) {
			if (rayClear(l, o, new Vec3(pts[i], pts[i + 1], pts[i + 2]))) return true;
		}
		return false;
	}

	/** Anti-ESP was switched off (command or config) while pairs were hidden: put every hidden entity back for its viewer, then drop the state. Runs on the next tick after the switch. */
	private static void restoreAll(MinecraftServer srv) {
		for (ServerLevel l : srv.getAllLevels()) {
			var cm = l.getChunkSource().chunkMap;
			for (ServerPlayer v : l.players()) for (Entity e : l.getAllEntities()) {
				if (!ESP.hidden(v.getId(), e.getId())) continue;
				ESP.forget(v.getId(), e.getId()); // forget first: isHidden() must be false when updatePlayer runs, or the mixin removes it again
				show(cm, v, e);
			}
		}
		ESP.clear(); // pairs for entities that are gone or viewers that left
	}
	// ---------- candidate search ----------
	/**
	 * Which entities may a viewer need culling decisions for? Before: every loaded entity of the level, for every viewer, every cycle.
	 * Now: the entities in the chunk map's tracked set (an entity nobody can be paired with needs no decision), filtered once per tick to cullable ones, indexed in an EntityGrid, and
	 * queried per viewer for the 128 block radius plus a margin. A hidden entity stays in the tracked set (hiding only removes the viewer from seenBy), so it is still re-checked.
	 * Fallback: any failure in building the index, or an index that is empty while the old path finds cullable entities, switches to l.getAllEntities() for the rest of the run
	 * and logs once. The margin covers movement between the build (start of this tick) and the exact test, which uses the live position.
	 * NEEDS A REAL SERVER PROFILE: the saving depends on how many loaded entities are untracked (mob caps, simulation vs tracking distance) and on grid build cost at real entity counts.
	 */
	private static final int QUERY_MARGIN = 8;
	private static final EntityGrid GRID = new EntityGrid();
	private static Entity[] gridEnt = new Entity[1024];
	private static int[] queryBuf = new int[1024];
	private static volatile boolean trackedIndexOk = true;
	private static ServerLevel gridLevel; private static long gridTick = -1;
	/** Build (once per level per tick) the grid from the tracked set. Returns false when the fallback must be used. */
	private static boolean buildGrid(ServerLevel l, long tick) {
		if (!trackedIndexOk) return false;
		if (gridLevel == l && gridTick == tick) return true;
		try {
			GRID.clear(); int n = 0;
			for (Object o : ((ChunkMapAccess) l.getChunkSource().chunkMap).meshac$entityMap().values()) {
				Entity e = ((TrackedEntityAccess) o).meshac$entity();
				if (e == null || e.isRemoved() || e.level() != l || !cullable(e)) continue;
				int idx = GRID.add(e.getId(), e.getX(), e.getZ());
				if (idx >= gridEnt.length) gridEnt = java.util.Arrays.copyOf(gridEnt, gridEnt.length * 2);
				gridEnt[idx] = e; n++;
			}
			gridLevel = l; gridTick = tick;
			if (n == 0 && tick % 1200 == 0 && !l.players().isEmpty()) { // sanity: an empty index is suspicious only if the old path finds cullable entities
				for (Entity e : l.getAllEntities()) if (cullable(e) && !(e instanceof ServerPlayer)) { trackedFail("tracked set empty but level has " + e.getType() + " entities"); return false; }
			}
			return true;
		} catch (Throwable t) { trackedFail(t.toString()); return false; }
	}
	private static void trackedFail(String why) {
		if (trackedIndexOk) { trackedIndexOk = false; dev.meshac.Meshac.LOG.warn("[meshac] anti-ESP tracked-set index disabled, using the full entity scan: {}", why); }
		gridLevel = null;
	}
	private static Entity[] candBuf = new Entity[1024];
	private static int candidatesFor(ServerLevel l, ServerPlayer v, long tick) {
		if (buildGrid(l, tick)) {
			int n = GRID.near(v.getX(), v.getZ(), 128 + QUERY_MARGIN, queryBuf);
			if (n > queryBuf.length) { queryBuf = new int[Math.max(n, queryBuf.length * 2)]; n = GRID.near(v.getX(), v.getZ(), 128 + QUERY_MARGIN, queryBuf); }
			if (n > candBuf.length) candBuf = new Entity[Math.max(n, candBuf.length * 2)];
			for (int i = 0; i < n; i++) candBuf[i] = gridEnt[queryBuf[i]];
			return n;
		}
		int n = 0; // fallback: the old full scan
		for (Entity e : l.getAllEntities()) { if (n == candBuf.length) candBuf = java.util.Arrays.copyOf(candBuf, n * 2); candBuf[n++] = e; }
		return n;
	}
	public static final VeilStats STATS = new VeilStats();
	public static String statusLine() { return STATS.line(ESP.hiddenPairs(), ESP.size(), trackedIndexOk); }
	/** Run at start and reload when anti-ESP is on. A failed check turns anti-ESP off: fail open. */
	public static void startupCheck() {
		STATS.reset(); trackedIndexOk = true; gridLevel = null;
		if (!dev.meshac.Config.get().veilEnabled() || !espOn) return;
		String bad = VeilStats.selfCheck();
		if (bad == null) dev.meshac.Meshac.LOG.info("[meshac] anti-ESP self-check passed");
		else { espOn = false; dev.meshac.Meshac.LOG.error("[meshac] anti-ESP self-check FAILED, anti-ESP is off: {}", bad); }
	}
	/** /mesh veil esp on|off. Turning on reruns the self-check and only switches on when it passes. Returns null when fine, otherwise the reason it stayed off. */
	public static String setEsp(boolean on) {
		if (!on) { espOn = false; return null; }
  if (!dev.meshac.Config.get().veilEnabled()) { espOn = false; return "veilEnabled is false; enable it in config and reload first"; }
		String bad = VeilStats.selfCheck();
		if (bad != null) { espOn = false; dev.meshac.Meshac.LOG.error("[meshac] anti-ESP self-check FAILED, anti-ESP stays off: {}", bad); return bad; }
		STATS.reset(); trackedIndexOk = true; gridLevel = null; espOn = true;
		dev.meshac.Meshac.LOG.info("[meshac] anti-ESP self-check passed");
		return null;
	}
	private static java.util.Set<Integer> onlineIds(MinecraftServer srv) { java.util.Set<Integer> ids = new java.util.HashSet<>(); for (ServerPlayer p : srv.getPlayerList().getPlayers()) ids.add(p.getId()); return ids; }
	private static int rr = 0;
	/** Called once per server tick (END_SERVER_TICK). Every pair is checked once per TICK_PERIOD, budgeted. */
	public static void tick(MinecraftServer srv) {
		srvRef = srv;
		if (!dev.meshac.Config.get().veilEnabled() || !espOn) { if (ESP.anyHidden()) restoreAll(srv); return; }
		long tick = srv.getTickCount();
		if (tick % 1200 == 0) { ESP.dropGoneHidden(id -> { for (ServerLevel lv : srv.getAllLevels()) if (lv.getEntity(id) != null) return true; return false; }); /* VERIFY Level.getEntity(int) */ ESP.sweep(tick, 1200); GRACE.sweep(tick, JOIN_GRACE_TICKS); EPOCH.retainOnly(onlineIds(srv)); } // pairs of dead entities and gone viewers were never removed before: the table only grew
		int budget = RAYS_PER_TICK; final long t0 = System.nanoTime(); final int budget0 = budget; int seen = 0, skipped = 0, hn = 0, sn = 0; boolean fb = false;
		for (ServerLevel l : srv.getAllLevels()) {
			var cm = l.getChunkSource().chunkMap;
			for (ServerPlayer v : l.players()) {
				if ((v.getId() + tick) % TICK_PERIOD != 0) continue; // stagger players across ticks
				if (EPOCH.observe(v.getId(), System.identityHashCode(l))) GRACE.start(v.getId(), tick);
				int nCand = candidatesFor(l, v, tick); seen += nCand; if (!trackedIndexOk) fb = true;
				for (int ci = 0; ci < nCand; ci++) {
					Entity e = candBuf[ci];
					if (!cullable(e) || e == v || e.level() != l || e.isRemoved()) continue;
					boolean wasHidden = ESP.hidden(v.getId(), e.getId());
					if (budget <= 0 && !wasHidden) { skipped++; continue; } // out of budget: skip NEW culls only. The old "return" left already hidden entities hidden (and starved every later viewer) until the next tick had budget, which under load meant until relog.
					double d = Math.sqrt(e.getBoundingBox().distanceToSqr(v.getEyePosition()));
					if (d > 128) continue;
					boolean ex = exempt(v, e, tick);
					boolean vis = ex || d <= 8 || visible(l, v, e);
					if (!ex && d > 8 && !wasHidden) budget--; // re-show checks of hidden pairs are free: they must never be skipped
					var a = ESP.update(v.getId(), e.getId(), tick, d, ex, vis);
					if (a == EspCore.Action.HIDE) { hide(cm, v, e); hn++; }
					else if (a == EspCore.Action.SHOW) { show(cm, v, e); sn++; }
				}
			}
		}
		STATS.tick(tick, System.nanoTime() - t0, seen, budget0 - budget, hn, sn, skipped, fb);
	}
	// TrackedEntity access goes through TrackedEntityAccess (mixin accessor to ChunkMap.entityMap).
	private static dev.meshac.veil.TrackedEntityAccess tracked(Object chunkMap, int id) { return (dev.meshac.veil.TrackedEntityAccess) ((dev.meshac.veil.ChunkMapAccess) chunkMap).meshac$entityMap().get(id); }
	private static void hide(Object cm, ServerPlayer v, Entity e) { var t = tracked(cm, e.getId()); if (t != null) t.meshac$remove(v); }
	private static void show(Object cm, ServerPlayer v, Entity e) { var t = tracked(cm, e.getId()); if (t != null) t.meshac$update(v); }
}
