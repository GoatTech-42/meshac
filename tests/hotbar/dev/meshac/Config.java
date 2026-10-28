package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
/** Test boundary: models the existing resolved owner exemption, not config parsing. */
public final class Config {
    private static final Config CURRENT = new Config();
    public static Config get() { return CURRENT; }
    public boolean exempt(ServerPlayer player) { return player.exempt; }
}
