package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
/** Private rig diagnostics only. Log-only, exact opt-in; never feeds checks or Verdict. */
public final class ActionTrace {
 public static final boolean ON = "1".equals(System.getenv("MESHAC_ACTION_TRACE"));
 private static final ActionTraceBudget BUDGET = new ActionTraceBudget();
 public static void event(ServerPlayer p, String kind, java.util.function.Supplier<String> fields) {
  if (!ON || !BUDGET.take(System.nanoTime()/1_000_000L)) return;
  try { Meshac.LOG.info("[action-trace] monoNs={} wallMs={} tick={} clientTick={} uuid={} kind={} dropped={} {}",
   System.nanoTime(),System.currentTimeMillis(),p.level().getServer().getTickCount(),Ticks.n(p),p.getUUID(),kind,BUDGET.dropped(),ActionTraceBudget.text(fields.get())); } catch (RuntimeException ignored) { /* diagnostics must not change gameplay */ }
 }
 /** Version-safe diagnostics only:26.x renamed ClickType to ContainerInput. */
 public static String clickKind(Object packet) {
  for (String name : new String[]{"containerInput", "clickType", "getClickType"}) {
   try {return String.valueOf(packet.getClass().getMethod(name).invoke(packet));}
   catch (ReflectiveOperationException ignored) {}
  }
  return "UNKNOWN_ACCESSOR";
 }
 public static void dig(ServerPlayer p, String phase, BlockPos pos) {
  if (!ON) return;
  event(p,"dig-"+phase,()-> {var b=p.level().getBlockState(pos);return "pos="+pos+" state="+b+" hardness="+b.getDestroySpeed(p.level(),pos)+" progress="+b.getDestroyProgress(p,p.level(),pos)+" tool="+p.getMainHandItem()+" components="+p.getMainHandItem().getComponents()+" effects="+p.getActiveEffects()+" ground="+p.onGround()+" water="+p.isInWater();});
 }
}
