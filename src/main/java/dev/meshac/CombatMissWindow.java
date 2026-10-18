package dev.meshac;

/** Separate-tick, same-target aim evidence. Packet batching is one observation. */
public final class CombatMissWindow {
	private int target=Integer.MIN_VALUE, lastTick=Integer.MIN_VALUE, count;
	private long lastNs;
 private Object identity;
	public boolean observe(int entity, int tick, long ns, boolean aimAccepted) {
  return observe(Integer.valueOf(entity), entity, tick, ns, aimAccepted);
 }
 public boolean observe(Object stableIdentity, int entity, int tick, long ns, boolean aimAccepted) {
		if(aimAccepted){reset();return false;}
		if(!java.util.Objects.equals(identity, stableIdentity) || target!=entity || ns<lastNs || ns-lastNs>4_000_000_000L || tick<lastTick){reset();target=entity;identity=stableIdentity;}
		if(lastTick==tick)return false;
		lastTick=tick;lastNs=ns;
		if(++count>=4){reset();return true;}
		return false;
	}
	private void reset(){identity=null;target=Integer.MIN_VALUE;lastTick=Integer.MIN_VALUE;lastNs=0;count=0;}
}
