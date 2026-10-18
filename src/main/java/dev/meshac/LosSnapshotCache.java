package dev.meshac;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** A tick-local cache of historical LOS observations, never a current-world oracle. */
public final class LosSnapshotCache {
	public enum Result { VISIBLE, BLOCKED, UNKNOWN }
	private final int limit;
	private long tick = Long.MIN_VALUE;
	private final Map<Integer, Result> observations = new HashMap<>();
	public LosSnapshotCache(int limit) {
		if (limit < 1) throw new IllegalArgumentException("limit");
		this.limit = limit;
	}
	/** Caller reads this result as a snapshot of the first observation, not a later block state. */
	public Result sample(long atTick, int entity, boolean selected, BooleanSupplier observation) {
		if (tick != atTick) { tick = atTick; observations.clear(); }
		Result old = observations.get(entity);
		if (old != null) return old;
		if (!selected || observations.size() >= limit) return Result.UNKNOWN;
		Result result = observation.getAsBoolean() ? Result.VISIBLE : Result.BLOCKED;
		observations.put(entity, result);
		return result;
	}
	/** Round-robin selection so dense-farm iteration order cannot starve later targets. */
	public static boolean selected(long tick, int index, int count, int budget) {
		if (count <= budget) return true;
		long first = Math.floorMod(tick * (long) budget, count);
		return Math.floorMod(index - first, count) < budget;
	}
	public int size() { return observations.size(); }
}
