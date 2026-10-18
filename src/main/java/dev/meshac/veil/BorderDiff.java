package dev.meshac.veil;

import java.util.ArrayList;
import java.util.List;

/**
 * Retroactive part of leak 2, design only: after a replan, which BORDER positions changed state, so a few block updates can fix a chunk the client already has. Bounded: past
 * `cap` positions nothing is sent (fail open, the old behaviour). Positions are packed like Veil.Mod (x | y << 4 | z << 8) with the section index in the high bits. Pure.
 */
public final class BorderDiff {
	private BorderDiff() {}
	public record Result(List<Integer> nowHidden, List<Integer> nowShown, boolean capped) {}

	/** oldIdx and newIdx are the planned positions before and after. A position only in newIdx must now be sent as fake, one only in oldIdx as real. */
	public static Result diff(int[] oldIdx, int[] newIdx, int cap) {
		java.util.HashSet<Integer> o = new java.util.HashSet<>(), n = new java.util.HashSet<>();
		for (int i : oldIdx) if (BorderGuard.isBorder(i & 0xFFF)) o.add(i);
		for (int i : newIdx) if (BorderGuard.isBorder(i & 0xFFF)) n.add(i);
		List<Integer> hid = new ArrayList<>(), shown = new ArrayList<>();
		for (int i : n) if (!o.contains(i)) hid.add(i);
		for (int i : o) if (!n.contains(i)) shown.add(i);
		boolean capped = hid.size() + shown.size() > cap;
		if (capped) return new Result(List.of(), List.of(), true);
		java.util.Collections.sort(hid); java.util.Collections.sort(shown);
		return new Result(hid, shown, false);
	}
}
