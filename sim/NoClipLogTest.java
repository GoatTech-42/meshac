package dev.meshac;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Real Movement policy behavior against scripted collision shapes, not native physics. */
public class NoClipLogTest {
 static int checks;
 static void ck(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
 static double[] move(ServerPlayer player, double x) {
  TestClock.ms += 50; TestClock.ns += 50000000;
  return Movement.check(player, ServerboundMovePlayerPacket.pos(x, 70, 0, true));
 }
 static ServerPlayer fresh() {
  Level.reset(); Verdict.next = Verdict.Step.LOG; Verdict.signals.clear();
  var player = new ServerPlayer("noclip"); player.setPos(0, 70, 0);
  for (int i = 0; i < 40; i++) ck(move(player, 0) == null, "warmup must never correct");
  Verdict.signals.clear(); return player;
 }
 static long noclip() { return Verdict.signals.stream().filter(s -> s.startsWith("noclip ")).count(); }
 public static void main(String[] args) {
  String mode = args.length == 0 ? "body" : args[0];
  for (Verdict.Step step : Verdict.Step.values()) {
   boolean log = step == Verdict.Step.LOG;
   var p = fresh(); Verdict.next = step;
   double[] result = null;
   if (mode.equals("body")) {
    Level.solid = true;
    for (int i = 0; i < 10; i++) {
     result = move(p, 0);
     if (log || i < 9) ck(result == null, "inside-block LOG and prethreshold packets must not correct");
    }
   } else {
    if (mode.equals("path")) Level.shapes = java.util.List.of(
      new AABB(-20, 69, -20, 20, 70, 20), new AABB(0.8, 70, -1, 1.2, 72, 1));
    result = move(p, mode.equals("path") ? 2 : 11);
    if (log) ck(result == null, "noclip " + mode + " LOG must not correct");
   }
   ck(noclip() == 1, "noclip " + mode + " must still signal exactly once");
   if (!log) ck(result != null && result[0] == 0 && result[1] == 70 && result[2] == 0,
     "enforcing noclip " + mode + " must retain safe-position setback");
   Movement.forget(p.getUUID());
  }
  var pending = fresh(); Movement.awaiting(pending, true);
  ck(move(pending, 11) == null && noclip() == 0, "pending server teleport must remain exempt from long-move noclip");
  Movement.forget(pending.getUUID());
  System.out.println("PASS noclip-policy " + mode + " checks=" + checks);
 }
}
