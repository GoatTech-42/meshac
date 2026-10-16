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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Anti-xray, section-copy variant. The packet constructor calls calculateChunkSize(chunk) to size the buffer and then extractChunkData(buf, chunk) to fill it.
 * For each section with buried hidden blocks we build ONE veiled private copy during the size pass and keep it in a per-packet plan; the write pass writes that same copy.
 * Size and bytes come from the same object, and the live section is never modified.
 * Targets checked in the server jars of 1.21.11, 26.1.2, 26.2 and 26.3: ClientboundLevelChunkPacketData.calculateChunkSize(LevelChunk)I, extractChunkData(FriendlyByteBuf, LevelChunk)V,
 * LevelChunkSection.getSerializedSize()I, write(FriendlyByteBuf)V and copy() all exist. UNCOMPILED, see TEST-PLAN.
 */
@Mixin(ClientboundLevelChunkPacketData.class)
public abstract class ChunkDataMixin {
	/** Per packet: the chunk, and the veiled copy (or null = send live) for each section index. */
	@Unique private static final class Plan { final LevelChunk chunk; final LevelChunkSection[] veiled; Plan(LevelChunk c) { chunk = c; veiled = new LevelChunkSection[c.getSections().length]; } }
	@Unique private static final ThreadLocal<Plan> meshac$plan = new ThreadLocal<>();
	@Unique private static final ThreadLocal<int[]> meshac$idx = ThreadLocal.withInitial(() -> new int[1]);

	// ---- size pass ----
	@Inject(method = "calculateChunkSize", at = @At("HEAD"))
	private static void meshac$szBegin(LevelChunk chunk, CallbackInfoReturnable<Integer> ci) { meshac$plan.set(new Plan(chunk)); meshac$idx.get()[0] = 0; }

	@Redirect(method = "calculateChunkSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;getSerializedSize()I"))
	private static int meshac$size(LevelChunkSection sec) {
		Plan p = meshac$plan.get();
		int i = meshac$idx.get()[0]++;
		if (p == null || i >= p.veiled.length) return sec.getSerializedSize();
		Veil.Mod[] m = Veil.mods(p.chunk, i, sec);
		LevelChunkSection v = m == null || m.length == 0 ? null : Veil.veiled(sec, m);
		p.veiled[i] = v;
		return (v != null ? v : sec).getSerializedSize();
	}

	// ---- write pass ----
	@Inject(method = "extractChunkData", at = @At("HEAD"))
	private static void meshac$begin(FriendlyByteBuf buf, LevelChunk chunk, CallbackInfo ci) { meshac$idx.get()[0] = 0; }

	@Inject(method = "extractChunkData", at = @At("RETURN"))
	private static void meshac$end(FriendlyByteBuf buf, LevelChunk chunk, CallbackInfo ci) { meshac$plan.remove(); }

	@Redirect(method = "extractChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;write(Lnet/minecraft/network/FriendlyByteBuf;)V"))
	private static void meshac$write(LevelChunkSection sec, FriendlyByteBuf buf) {
		Plan p = meshac$plan.get();
		int i = meshac$idx.get()[0]++;
		LevelChunkSection v = p == null || i >= p.veiled.length ? null : p.veiled[i];
		(v != null ? v : sec).write(buf);
	}
}
