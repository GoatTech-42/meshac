package dev.meshac.mixin;
import dev.meshac.Fingerprints;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
 @Shadow @Final protected MinecraftServer server;
 @Shadow public abstract com.mojang.authlib.GameProfile getOwner();
 @Inject(method="handleCustomPayload",at=@At("HEAD"))
 private void meshac$identity(ServerboundCustomPayloadPacket packet,CallbackInfo ci) {
  if(packet.payload() instanceof BrandPayload brand) { var id=getOwner().id(); var value=brand.brand(); if(server.isSameThread()) Fingerprints.brand(id,value); else server.execute(() -> Fingerprints.brand(id,value)); }
 }
}
