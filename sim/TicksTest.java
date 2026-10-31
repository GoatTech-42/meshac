package dev.meshac;

import net.minecraft.server.level.ServerPlayer;

/** Scripted clocks exercise real Ticks, not packet hooks or thread ordering. */
public class TicksTest {
 static int checks;
 static void ck(boolean value, String message) {
  checks++;
  if (!value) throw new AssertionError(message);
 }
 static void advance(long ms) { TestClock.ms += ms; }
 static void inc(ServerPlayer p, long gap) { advance(gap); Ticks.inc(p); }
 public static void main(String[] args) {
  var a = new ServerPlayer();
  ck(Ticks.n(a) == 0 && !Ticks.active(a) && !Ticks.bunched(a), "unseen player must leave timing rules off");
  for (int i = 0; i < 40; i++) inc(a, 50);
  ck(Ticks.n(a) == 40 && !Ticks.active(a), "40 ticks must not activate timing rules");
  inc(a, 50);
  ck(Ticks.n(a) == 41 && Ticks.active(a), "41st recent tick must activate timing rules");
  ck(!Ticks.bunched(a), "ordinary tick spacing must not open backlog grace");
  advance(499);
  ck(Ticks.active(a), "499ms old tick must remain active");
  advance(1);
  ck(!Ticks.active(a), "500ms old tick must turn timing rules off");
  inc(a, 50);
  ck(Ticks.active(a), "fresh tick must restore timing rules");

  var b = new ServerPlayer();
  inc(b, 50); inc(b, 1); inc(b, 1);
  ck(!Ticks.bunched(b), "three close tick packets must not start backlog grace");
  inc(b, 1);
  ck(Ticks.bunched(b), "four close tick packets must start backlog grace");
  ck(!Ticks.bunched(a), "backlog grace must be scoped to player UUID");
  advance(2999);
  ck(Ticks.bunched(b), "backlog grace must last through 2999ms");
  advance(1);
  ck(!Ticks.bunched(b), "backlog grace must expire at 3000ms");
  // A normal gap breaks an incomplete backlog, including exactly five ms.
  var c = new ServerPlayer();
  inc(c, 50); inc(c, 1); inc(c, 1); inc(c, 5); inc(c, 1);
  ck(!Ticks.bunched(c), "five ms gap must break the close-packet streak");
  inc(c, 1); inc(c, 1);
  ck(Ticks.bunched(c), "new four-packet streak must open grace after reset");

  Ticks.forget(a.getUUID());
  ck(Ticks.n(a) == 0 && !Ticks.active(a) && !Ticks.bunched(a), "forget must reset count and activity");
  ck(Ticks.bunched(c), "forgetting another player must preserve backlog grace");
  Ticks.forget(c.getUUID());
  ck(Ticks.n(c) == 0 && !Ticks.bunched(c), "forget must clear pending backlog grace");
  inc(c, 50);
  ck(Ticks.n(c) == 1 && !Ticks.active(c) && !Ticks.bunched(c), "forgotten identity must restart cold");
  System.out.println("PASS tick-counter checks=" + checks);
 }
}
