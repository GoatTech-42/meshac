package dev.meshac.mixin;
import dev.meshac.Fingerprints;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin {
 @Shadow @Final private com.mojang.authlib.GameProfile gameProfile;
 @Inject(method="handleConfigurationFinished",at=@At("HEAD"))
 private void meshac$channels(ServerboundFinishConfigurationPacket packet,CallbackInfo ci) {
  var handler=(ServerConfigurationPacketListenerImpl)(Object)this;
  java.util.Set<String> values=new java.util.HashSet<>();
  for(var id:net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking.getSendable(handler)) values.add(id.toString());
  Fingerprints.channels(gameProfile.id(),values);
 }
}
