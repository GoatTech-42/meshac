package dev.meshac;

import java.util.Optional;
import java.util.concurrent.Callable;

/** Parse a replacement value first; the caller swaps it in only when this returns a value, so a bad file leaves the prior settings in force. */
public final class SafeSwap {
	private SafeSwap() {}
	public static <T> Optional<T> parse(Callable<T> parser) {
		try { return Optional.ofNullable(parser.call()); } catch (Exception | StackOverflowError e) { return Optional.empty(); }
	}
}
