package dev.meshac;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Who is a Bedrock player, for the checks that skip them. No Minecraft types and no compile time dependency on Floodgate.
 * Rule 1: UUID high half zero. That is the UUID Floodgate gives an unlinked Bedrock player (FloodgateApi.createJavaPlayerId(xuid)). No Java client can have it: an offline-mode UUID always carries the version bits in its high half.
 * Rule 2: Floodgate says so. A linked Bedrock player gets the Java account's UUID, which does not look like rule 1, but FloodgateApi.isFloodgatePlayer(UUID) is true for linked accounts too
 * (its javadoc: isFloodgateId cannot validate a linked UUID, use isFloodgatePlayer to include them). The API is reached by reflection and only when Floodgate is on the server; without it only rule 1 applies.
 * A Java client cannot fake rule 2: Floodgate only registers players that came through its own handshake. Names are never looked at, so a "." prefix, a configured prefix or any other name buys nothing.
 * Both answers expire. A "no" after NEG_TTL_MS so Floodgate can register a player late. A "yes" after POS_TTL_MS because Floodgate keeps a just-disconnected linked player for a short while (its pendingRemove cache), and a Java session of the same account that starts inside that window must not be skipped for good.
 * When Floodgate cannot answer (reloading, not ready, throwing) a "yes" already known stays a "yes": a linked player does not turn into a Java client for the length of a reload. A player with no known answer is Java.
 * The Floodgate instance is fetched on every question, not kept: a Floodgate reload makes a new instance and an old reference would answer "no" for every player that joins after it.
 */
public final class BedrockIds {
	public interface Probe { boolean isFloodgatePlayer(UUID id) throws Exception; }
	/** Floodgate is there but has no API instance right now (starting or reloading). Not an error: asked again soon, known answers kept. */
	public static final class NotReady extends Exception { public NotReady() { super("floodgate api instance not available", null, false, false); } }
	public static final long NEG_TTL_MS = 5000, POS_TTL_MS = 60000, ERROR_BACKOFF_MS = 30000, NOT_READY_MS = 5000;
	private static final int MAX = 4096;

	/** The real instance, wired to Floodgate by reflection. */
	public static final BedrockIds DEFAULT = new BedrockIds(() -> loadFloodgate("org.geysermc.floodgate.api.FloodgateApi"), System::currentTimeMillis);

	private static final class E { final boolean yes; final long until; E(boolean y, long u) { yes = y; until = u; } }
	private final Supplier<Probe> loader; private final LongSupplier clock;
	private final ConcurrentHashMap<UUID, E> cache = new ConcurrentHashMap<>();
	private volatile Probe probe; private volatile boolean absent; private volatile long retryAt;
	private volatile java.util.function.Consumer<String> log = m -> {};
	/** One line when the Floodgate API is found or missing, and one the first time a non-zero-high UUID is answered yes (a linked account). For the live check: nothing else in Meshac says whether this works. */
	public void onLog(java.util.function.Consumer<String> l) { log = l; }
	/** loader returns the probe, null when Floodgate is there but not started yet (asked again later), and throws UnsupportedOperationException when it is not there at all (never asked again). */
	public BedrockIds(Supplier<Probe> loader, LongSupplier clock) { this.loader = loader; this.clock = clock; }

	public boolean isBedrock(UUID id) {
		if (id.getMostSignificantBits() == 0L) return true;
		long now = clock.getAsLong();
		E c = cache.get(id);
		if (c != null && now < c.until) return c.yes;
		return ask(id, now, c != null && c.yes);
	}

	private boolean ask(UUID id, long now, boolean knownYes) {
		if (absent) return false;
		Probe p = probe;
		if (p == null) {
			if (now < retryAt) return knownYes;
			synchronized (this) {
				if (probe == null) {
					try { probe = loader.get(); }
					catch (UnsupportedOperationException e) { absent = true; log.accept("floodgate api not found, Bedrock skip uses the zero-high UUID rule only"); return false; }
					catch (RuntimeException | LinkageError e) { probe = null; }
					if (probe == null) { retryAt = now + NOT_READY_MS; return knownYes; }
					log.accept("floodgate api found, asking it about non-Bedrock-looking UUIDs");
				}
				p = probe;
			}
		}
		boolean yes;
		try { yes = p.isFloodgatePlayer(id); }
		catch (NotReady e) { return knownYes; } // reloading: keep what we knew, ask again on the next packet
		catch (Exception | LinkageError e) { probe = null; retryAt = now + ERROR_BACKOFF_MS; return knownYes; }
		if (cache.size() > MAX) cache.clear();
		E was = cache.put(id, new E(yes, now + (yes ? POS_TTL_MS : NEG_TTL_MS)));
		if (yes && (was == null || !was.yes) && !knownYes) log.accept("Floodgate player with a normal UUID (linked account) treated as Bedrock: " + id);
		return yes;
	}

	/** Join and disconnect: a new session starts with no remembered answer. */
	public void forget(UUID id) { cache.remove(id); }
	public int size() { return cache.size(); }
	public boolean floodgateAbsent() { return absent; }

	/** Reflective Floodgate hook: Class org.geysermc.floodgate.api.FloodgateApi, static getInstance(), instance isFloodgatePlayer(UUID). Names confirmed with javap on ilhsmc's Floodgate 2.2.6-b67 jar. */
	public static Probe loadFloodgate(String apiClass) {
		try {
			Class<?> c = Class.forName(apiClass, true, BedrockIds.class.getClassLoader());
			Method get = c.getMethod("getInstance");
			Method is = c.getMethod("isFloodgatePlayer", UUID.class);
			if (get.invoke(null) == null) return null; // Floodgate present but not started yet
			return id -> {
				Object inst = get.invoke(null); // fetched every time: a reload replaces the instance
				if (inst == null) throw new NotReady();
				try { return (Boolean) is.invoke(inst, id); }
				catch (InvocationTargetException e) { if (e.getCause() instanceof Exception) throw (Exception) e.getCause(); throw e; }
			};
		} catch (ClassNotFoundException | NoSuchMethodException e) { throw new UnsupportedOperationException(e); } // not installed, or an API we do not know
		catch (ReflectiveOperationException e) { return null; }
	}
}
