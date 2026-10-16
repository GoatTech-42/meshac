package dev.meshac.veil;

import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

/**
 * Per-packet plan, pure Java so the size/bytes invariant can be tested with plain javac.
 * The size pass decides, per section, whether a veiled replacement is used and remembers it. The write pass takes exactly that replacement.
 * Invariant: for every section, the object whose size was counted is the object that is written.
 */
public final class PacketPlan<S> {
	private final Object[] veiled;
	public PacketPlan(int sections) { veiled = new Object[sections]; }

	/** Size pass for section i. veil returns the replacement or null for "use live". Exceptions in veil mean live (fail open). */
	public int size(int i, S live, UnaryOperator<S> veil, ToIntFunction<S> sizer) {
		S v = null;
		if (i >= 0 && i < veiled.length) {
			try { v = veil.apply(live); } catch (RuntimeException e) { v = null; }
			veiled[i] = v;
		}
		return sizer.applyAsInt(v != null ? v : live);
	}

	/** Write pass for section i: the object to write. */
	@SuppressWarnings("unchecked")
	public S forWrite(int i, S live) {
		S v = i >= 0 && i < veiled.length ? (S) veiled[i] : null;
		return v != null ? v : live;
	}
}
