package dev.meshac.veil;

import java.util.function.IntFunction;

/** Pure freshness test for a cached chunkveil plan (no Minecraft types): every planned position must still hold the state the plan was computed from. Veil.fresh delegates here. */
public final class PlanFresh {
	private PlanFresh() {}
	/** @param n entries; @param idx position of entry i; @param real state the plan expects there; @param live state the section holds now (by position). Reference equality, like BlockState. */
	public static boolean fresh(int n, IntFunction<Integer> idx, IntFunction<Object> real, IntFunction<Object> live) {
		for (int i = 0; i < n; i++) if (live.apply(idx.apply(i)) != real.apply(i)) return false;
		return true;
	}
}
