package dev.meshac;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Client-supplied hints only; never add behavior heat. Disabled with empty exact lists. */
public final class Fingerprints {
 private static final Map<UUID,String> BRANDS = new ConcurrentHashMap<>();
 private static final Map<UUID,Set<String>> CHANNELS = new ConcurrentHashMap<>();
 private static final Set<UUID> REMOVED = ConcurrentHashMap.newKeySet();
 public static void brand(UUID id, String value) { if(value.length() <= 256) BRANDS.put(id,value); }
 public static void channels(UUID id, Set<String> values) { CHANNELS.put(id,new HashSet<>(values)); }
 static boolean safeBrand(String value) { return value != null && !value.isBlank() && !Set.of("vanilla","fabric","forge","neoforge").contains(value.toLowerCase(Locale.ROOT)); }
 static boolean safeChannel(String value) { return value != null && value.contains(":") && !value.startsWith("minecraft:") && !value.startsWith("fabric:") && !value.startsWith("fabric-") && !value.startsWith("forge:") && !value.startsWith("neoforge:") && !value.startsWith("c:"); }
 public static void check(ServerPlayer pl) {
  Config cf=Config.get(); if(!cf.clientFingerprintEnabled || REMOVED.contains(pl.getUUID())) return;
  String brand=BRANDS.get(pl.getUUID()), match=null;
  if(cf.clientFingerprintBrands != null) for(String rule:cf.clientFingerprintBrands) if(safeBrand(rule) && rule.equals(brand)) {match="brand="+rule;break;}
  Set<String> values=new HashSet<>(CHANNELS.getOrDefault(pl.getUUID(),Set.of()));
  for(var c:net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.getSendable(pl)) values.add(c.toString());
  if(match==null && cf.clientFingerprintChannels != null) for(String rule:cf.clientFingerprintChannels) if(safeChannel(rule) && values.contains(rule)) {match="channel="+rule;break;}
  if(match==null || !REMOVED.add(pl.getUUID())) return;
  boolean again=false; long since=System.currentTimeMillis()-cf.memoryDays()*86400000L;
  for(Cases.Case c:Cases.of(pl.getGameProfile().name())) if(c.uuid.equals(pl.getUUID().toString()) && c.by.equals("meshac-fingerprint") && !c.pardoned && c.at>=since) again=true;
  String reason="We detected that you might be using a hacked client. Client identity can be spoofed; contact staff if this is a mistake.";
  Cases.Case c=Cases.add(pl.getGameProfile().name(),pl.getUUID(),again?"ban":"kick",reason,"meshac-fingerprint",0,List.of("Client-supplied hint, not behavior proof",match));
  Meshac.LOG.warn("[meshac] FINGERPRINT {} {} {} case={}",pl.getGameProfile().name(),match,c.action,c.id);
  pl.connection.disconnect(again?Screens.ban(c):Screens.kick(reason,c));
 }
 public static void forget(UUID id) { BRANDS.remove(id); CHANNELS.remove(id); REMOVED.remove(id); }
}
