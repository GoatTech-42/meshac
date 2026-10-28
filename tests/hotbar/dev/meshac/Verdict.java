package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
/** Captures the signal at Inventory's output boundary without executing punishment. */
public final class Verdict {
    public static int signals;
    public static void signal(ServerPlayer player, String check, String detail, int weight) {
        if (!check.equals("inventory") || weight != 1) throw new AssertionError("Wrong signal contract");
        signals++;
    }
}
