package net.minecraft.server.level;
import java.util.UUID;
public final class ServerPlayer {
    private final UUID id = UUID.randomUUID();
    public boolean creative, spectator, exempt;
    public UUID getUUID() { return id; }
    public boolean isCreative() { return creative; }
    public boolean isSpectator() { return spectator; }
}
