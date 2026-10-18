import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stand-in for org.geysermc.floodgate.api.FloodgateApi: same static getInstance() and instance isFloodgatePlayer(UUID) shape. */
public class FakeFloodgateApi {
	public static FakeFloodgateApi instance;
	public static final Set<UUID> players = ConcurrentHashMap.newKeySet();
	public static boolean throwing;
	/** false: this instance answers from its own set, like a new Floodgate instance after a reload that has only seen the players that joined since. */
	public boolean useShared = true;
	public final Set<UUID> mine = ConcurrentHashMap.newKeySet();
	public int calls;
	public static FakeFloodgateApi getInstance() { return instance; }
	public boolean isFloodgatePlayer(UUID id) { calls++; if (throwing) throw new IllegalStateException("boom"); return useShared ? players.contains(id) : mine.contains(id); }
}
