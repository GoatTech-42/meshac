package dev.meshac.mixin;

import dev.meshac.veil.BlockEntityStrip;
import dev.meshac.veil.Veil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
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
	/** Per packet: the chunk and the pure plan (see dev.meshac.veil.PacketPlan, unit tested in sim/veil/SizeBytesInvariantTest). */
	@Unique private static final class Plan { final LevelChunk chunk; final dev.meshac.veil.PacketPlan<LevelChunkSection> plan; final BlockEntityStrip.Encoded encoded; Plan(LevelChunk c) { chunk = c; plan = new dev.meshac.veil.PacketPlan<>(c.getSections().length); encoded = new BlockEntityStrip.Encoded(c.getSections().length); } }
	@Unique private static final ThreadLocal<Plan> meshac$plan = new ThreadLocal<>();
	@Unique private static final ThreadLocal<int[]> meshac$idx = ThreadLocal.withInitial(() -> new int[1]);

	// ---- block entity list ----
	/** The packet's block entity list (private final java.util.List<ClientboundLevelChunkPacketData$BlockEntityInfo> in 1.21.11, 26.1.2, 26.2 and 26.3, an ArrayList built in the constructor). */
	@Shadow @Final @Mutable private List<Object> blockEntitiesData;

	/**
	 * A hidden chest or spawner is stone in the section we send, but its block entity entry (position and type) would still be in the list. Drop those entries.
	 * The constructor builds the list by walking chunk.getBlockEntities() in order, so entry i of the list is entry i of that walk (sizes must match, see BlockEntityStrip).
	 * Never touches BlockEntityInfo's fields (they differ across the four versions). A replacement is built privately, then the reference is swapped once. Failures before the swap leave the original list intact.
	 */
	@Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("RETURN"))
	private void meshac$stripBlockEntities(LevelChunk chunk, CallbackInfo ci) {
		try {
			Plan packet = meshac$plan.get();
   if (packet == null || packet.chunk != chunk) return;
			Map<BlockPos, BlockEntity> walk = chunk.getBlockEntities();
			if (walk.isEmpty()) return;
			List<BlockPos> order = new ArrayList<>(walk.keySet()); // same iteration order as the constructor's loop, if the map was not touched in between
   if (blockEntitiesData.size() != order.size()) { Veil.beSizeMismatch++; return; }
   List<Object> original = blockEntitiesData;
   List<Object> replacement = BlockEntityStrip.filtered(original, order.size(), i -> {
    BlockPos pos = order.get(i);
    return packet.encoded.hidden(chunk.getSectionIndex(pos.getY()), BlockEntityStrip.packed(pos.getX(), pos.getY(), pos.getZ()));
   });
   int removed = original.size() - replacement.size();
   blockEntitiesData = replacement; // single commit point: original was never changed
   Veil.beStripped += removed;
		} catch (Throwable t) { /* before reference swap, the original list remains intact */ }
  finally { meshac$plan.remove(); }
	}

	// ---- size pass ----
	@Inject(method = "calculateChunkSize", at = @At("HEAD"))
	private static void meshac$szBegin(LevelChunk chunk, CallbackInfoReturnable<Integer> ci) { meshac$plan.set(new Plan(chunk)); meshac$idx.get()[0] = 0; }

	@Redirect(method = "calculateChunkSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;getSerializedSize()I"))
	private static int meshac$size(LevelChunkSection sec) {
		Plan p = meshac$plan.get();
		int i = meshac$idx.get()[0]++;
		if (p == null) return sec.getSerializedSize();
		Veil.Mod[] m = Veil.mods(p.chunk, i, sec);
		return p.plan.size(i, sec, x -> {
   if (m == null || m.length == 0) return null;
   Veil.VeiledSection result = Veil.veiledResult(x, m);
   if (result == null) return null;
   p.encoded.copied(i, result.maskedEntities());
   return result.section();
  }, LevelChunkSection::getSerializedSize);
	}

	// ---- write pass ----
	@Inject(method = "extractChunkData", at = @At("HEAD"))
	private static void meshac$begin(FriendlyByteBuf buf, LevelChunk chunk, CallbackInfo ci) { meshac$idx.get()[0] = 0; Plan p = meshac$plan.get(); if (p != null && p.chunk != chunk) meshac$plan.remove(); } // a plan left over from another chunk (exception or reentrant construction) is never used

	@Inject(method = "extractChunkData", at = @At("RETURN"))
	private static void meshac$end(FriendlyByteBuf buf, LevelChunk chunk, CallbackInfo ci) { /* constructor RETURN consumes the exact written mask, then clears the plan */ }

	@Redirect(method = "extractChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;write(Lnet/minecraft/network/FriendlyByteBuf;)V"))
	private static void meshac$write(LevelChunkSection sec, FriendlyByteBuf buf) {
		Plan p = meshac$plan.get();
		int i = meshac$idx.get()[0]++;
		(p == null ? sec : p.plan.forWrite(i, sec)).write(buf);
  if (p != null) p.encoded.wrote(i);
	}
}
