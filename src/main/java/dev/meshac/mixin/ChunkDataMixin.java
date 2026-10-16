package dev.meshac.mixin;

import dev.meshac.veil.Veil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anti-xray: while the chunk is encoded into the packet, the buried hidden blocks are swapped for stone (or a fake ore) in the section, written, and put back.
 * Sections are touched with the no-lock setBlockState variant on the packet-building thread only; the world is restored before the method returns.
 * UNCOMPILED. Assumes extractChunkData loops the sections in order and calls section.write(buf) once per section.
 */
@Mixin(ClientboundLevelChunkPacketData.class)
public abstract class ChunkDataMixin {
	@Unique private static final ThreadLocal<LevelChunk> meshac$chunk = new ThreadLocal<>();
	@Unique private static final ThreadLocal<int[]> meshac$idx = ThreadLocal.withInitial(() -> new int[1]);

	@Inject(method = "extractChunkData", at = @At("HEAD"))
	private static void meshac$begin(FriendlyByteBuf buf, LevelChunk chunk, CallbackInfo ci) { meshac$chunk.set(chunk); meshac$idx.get()[0] = 0; }

	@Inject(method = "extractChunkData", at = @At("RETURN"))
	private static void meshac$end(FriendlyByteBuf buf, LevelChunk chunk, CallbackInfo ci) { meshac$chunk.remove(); meshac$plan.remove(); }

	@Redirect(method = "extractChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;write(Lnet/minecraft/network/FriendlyByteBuf;)V"))
	private static void meshac$write(LevelChunkSection sec, FriendlyByteBuf buf) {
		LevelChunk c = meshac$chunk.get();
		int i = meshac$idx.get()[0]++;
		Veil.Mod[] m = meshac$planFor(c, i, sec);
		if (m == null || m.length == 0) { sec.write(buf); return; }
		net.minecraft.world.level.block.state.BlockState[] orig = Veil.swap(sec, m);
		try { sec.write(buf); } finally { Veil.restore(sec, m, orig); }
	}

	/**
	 * One plan per packet: calculateChunkSize runs first and records the Mod[] it used for each section; extractChunkData then uses those exact arrays.
	 * Before, each phase looked the cache up itself, so an invalidation between them (or a stale entry) made the buffer size and the written bytes disagree.
	 */
	@Unique private static final ThreadLocal<Object[]> meshac$plan = new ThreadLocal<>(); // [LevelChunk, Veil.Mod[][]]
	@Unique private static Veil.Mod[] meshac$planFor(LevelChunk c, int i, LevelChunkSection sec) {
		if (c == null) return null;
		Object[] p = meshac$plan.get();
		if (p != null && p[0] == c) { Veil.Mod[][] a = (Veil.Mod[][]) p[1]; return i < a.length ? a[i] : null; }
		return null; // no size plan for this chunk: send it unveiled rather than risk a size mismatch (fail open)
	}

	// The packet buffer is sized from the REAL sections before anything is written. Swapping blocks can grow a palette, so size it from the swapped sections too.
	@Unique private static final ThreadLocal<LevelChunk> meshac$szChunk = new ThreadLocal<>();
	@Unique private static final ThreadLocal<int[]> meshac$szIdx = ThreadLocal.withInitial(() -> new int[1]);

	@Inject(method = "calculateChunkSize", at = @At("HEAD"))
	private static void meshac$szBegin(LevelChunk chunk, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> ci) {
		meshac$szChunk.set(chunk); meshac$szIdx.get()[0] = 0;
		meshac$plan.set(new Object[] { chunk, new Veil.Mod[chunk.getSections().length][] });
	}

	@Inject(method = "calculateChunkSize", at = @At("RETURN"))
	private static void meshac$szEnd(LevelChunk chunk, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> ci) { meshac$szChunk.remove(); }

	/** extractChunkData's own RETURN also clears the plan (see meshac$end). */
	@Redirect(method = "calculateChunkSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;getSerializedSize()I"))
	private static int meshac$size(LevelChunkSection sec) {
		LevelChunk c = meshac$szChunk.get();
		int i = meshac$szIdx.get()[0]++;
		Veil.Mod[] m = c == null ? null : Veil.mods(c, i, sec);
		Object[] p = meshac$plan.get();
		if (p != null && p[0] == c && i < ((Veil.Mod[][]) p[1]).length) ((Veil.Mod[][]) p[1])[i] = m;
		if (m == null || m.length == 0) return sec.getSerializedSize();
		net.minecraft.world.level.block.state.BlockState[] orig = Veil.swap(sec, m);
		try { return sec.getSerializedSize(); } finally { Veil.restore(sec, m, orig); }
	}
}
