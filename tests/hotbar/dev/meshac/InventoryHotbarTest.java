package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
public final class InventoryHotbarTest {
    public static void main(String[] args) {
        var p = new ServerPlayer();
        switch (args[0]) {
            case "creative" -> p.creative = true;
            case "spectator" -> p.spectator = true;
            case "exempt" -> p.exempt = true;
            case "survival" -> { }
            default -> throw new AssertionError("Unknown test");
        }
        // 31 calls cross the existing per-second limit. No sleeps or changed thresholds.
        for (int i = 0; i < 31; i++) Inventory.hotbar(p);
        int wanted = args[0].equals("survival") ? 1 : 0;
        if (Verdict.signals != wanted)
            throw new AssertionError(args[0] + ": expected " + wanted + " signals, got " + Verdict.signals);
        Inventory.forget(p.getUUID());
        System.out.println("PASS " + args[0]);
    }
}
