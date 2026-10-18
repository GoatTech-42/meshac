package dev.meshac.veil;

import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

/**
 * Per-packet plan, pure Java so the size/bytes invariant can be tested with plain javac.
 * The size pass decides, per section, whether a veiled replacement is used and remembers it. The write pass takes exactly that replacement.
 * Invariant: for every section, the object whose size was counted is the object that is written.
 */
public final class PacketPlan<S> {
	private final Object[] chosen; private final boolean[] sized;
	public PacketPlan(int sections) { chosen = new Object[sections]; sized = new boolean[sections]; }

	/** Size pass for section i. veil returns the replacement or null for "use live". Exceptions in veil mean live (fail open). */
	public int size(int i, S live, UnaryOperator<S> veil, ToIntFunction<S> sizer) {
		S v = null;
		if (i >= 0 && i < chosen.length) {
			try { v = veil.apply(live); } catch (RuntimeException e) { v = null; }
		}
		S sel = v != null ? v : live;
		if (i >= 0 && i < chosen.length) { chosen[i] = sel; sized[i] = true; } // remember the object that was sized, live or veiled, so the write pass writes exactly that one
		return sizer.applyAsInt(sel);
	}

	/** Write pass for section i: the object to write. */
	@SuppressWarnings("unchecked")
	public S forWrite(int i, S live) {
		return i >= 0 && i < chosen.length && sized[i] ? (S) chosen[i] : live; // a slot that was never sized falls back to the caller's section
	}
}
