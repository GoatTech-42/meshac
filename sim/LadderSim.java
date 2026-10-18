import dev.meshac.Ladder;
import java.util.*;
public class LadderSim {
  static Ladder.Params P = Ladder.Params.defaults();
  static String run(String name, long[][] flags, String[] checks, int[] w) {
    Ladder.Heat h = new Ladder.Heat(); Ladder.Step last = Ladder.Step.SETBACK; long firstRemove = -1;
    for (int i = 0; i < flags.length; i++) { Ladder.Step s = h.add(checks[i], w[i], flags[i][0], P); if (s == Ladder.Step.REMOVE && firstRemove < 0) { firstRemove = flags[i][0] - flags[0][0]; break; } last = s; }
    return String.format("%-44s %s", name, firstRemove < 0 ? "no removal (max step " + last + ")" : "REMOVED after " + firstRemove + " ms");
  }
  static long[][] t(long... ms) { long[][] a = new long[ms.length][1]; for (int i = 0; i < ms.length; i++) a[i][0] = 1_000_000 + ms[i]; return a; }
  public static void main(String[] x) {
    // live false-positive replays (flag times relative, from the 2:28 and 2:31 PM logs)
    System.out.println(run("live 2:28 wind-charge run (5 flags/21s)", t(0, 4000, 7000, 14000, 18000), new String[]{"interact","interact","timer","interact","interact"}, new int[]{1,1,1,1,1}));
    System.out.println(run("live 2:31 anchor swap burst (6 flags/2s)", t(0, 100, 300, 900, 1500, 1900), new String[]{"interact","interact","interact","interact","interact","interact"}, new int[]{1,1,1,1,1,1}));
    System.out.println(run("lag stall: 5 checks in one 40ms burst", t(0, 5, 10, 20, 40), new String[]{"interact","timer","look","speed","combat"}, new int[]{1,1,1,1,1}));
    System.out.println(run("lag stall hits tier-B/C x8 within 100ms", t(0,10,20,30,40,50,60,70), new String[]{"timer","interact","look","combat","speed","interact","timer","look"}, new int[]{1,2,1,1,1,1,1,1}));
    System.out.println(run("2:38 live scaffold: hotbar 15/s x3 + 7b/s", t(0, 1000, 1500, 2000), new String[]{"interact","inventory","inventory","interact"}, new int[]{1,1,1,1}));
    // hacks
    long[] fly = new long[10]; for (int i=0;i<10;i++) fly[i]=i*50L;
    System.out.println(run("fly: tier A every tick from 0 (live 2:51 = 3 flags)", t(0,50,100,150,200), new String[]{"fly","fly","fly","fly","fly"}, new int[]{3,3,3,3,3}));
    System.out.println(run("fly: 3 flags over 1 s", t(0,500,1000), new String[]{"fly","fly","fly"}, new int[]{3,3,3}));
    System.out.println(run("speed (tier C) 1 flag per tick x10", t(0,50,100,150,200,250,300,350,400,450), new String[]{"speed","speed","speed","speed","speed","speed","speed","speed","speed","speed"}, new int[]{1,1,1,1,1,1,1,1,1,1}));
    System.out.println(run("speed tier C, 1 flag/ 400ms x8", t(0,400,800,1200,1600,2000,2400,2800), new String[]{"speed","speed","speed","speed","speed","speed","speed","speed"}, new int[]{1,1,1,1,1,1,1,1}));
    System.out.println(run("killaura hits (weight 2) every 300ms", t(0,300,600,900), new String[]{"combat","combat","combat","combat"}, new int[]{2,2,2,2}));
    System.out.println(run("scaffold chain (2) + place speed (1) over 3s", t(0,1000,2000,3000,3500), new String[]{"interact","interact","interact","interact","inventory"}, new int[]{2,2,2,2,1}));
    // Monte Carlo: legit player, bursts of b correlated flags from one cause, rate lambda bursts/hour for 10 h, 200000 sessions
    Random r = new Random(7); int removed = 0, N = 200000;
    for (int s = 0; s < N; s++) {
      Ladder.Heat h = new Ladder.Heat(); double tms = 0; boolean rem = false;
      double rateMs = 1.0 / (1.0 / 3_600_000.0 * 20.0); // 20 bursts per hour (very noisy tunnel)
      while (tms < 10 * 3_600_000.0 && !rem) {
        tms += -Math.log(1 - r.nextDouble()) * rateMs;
        int b = 1 + r.nextInt(6); long base = (long) tms;
        for (int k = 0; k < b && !rem; k++) if (h.add("c" + r.nextInt(4), 1 + r.nextInt(2), base + k * 15L, P) == Ladder.Step.REMOVE) rem = true;
      }
      if (rem) removed++;
    }
    System.out.printf("Monte Carlo: %d sessions x 10 h, 20 lag bursts/h (1-6 flags, 4 checks, tiers C/B): %d removals (%.5f%%)%n", N, removed, 100.0 * removed / N);
    for (double perHour : new double[]{2, 20}) { removed = 0; r = new Random(11);
      for (int q = 0; q < N; q++) { Ladder.Heat h = new Ladder.Heat(); double tms = 0; boolean rem = false; double rateMs = 3_600_000.0 / perHour;
        while (tms < 10 * 3_600_000.0 && !rem) { tms += -Math.log(1 - r.nextDouble()) * rateMs; int b = 1 + r.nextInt(6); long base = (long) tms;
          for (int k = 0; k < b && !rem; k++) if (h.add("c" + r.nextInt(4), 1 + r.nextInt(2), base + k * 15L, P) == Ladder.Step.REMOVE) rem = true; }
        if (rem) removed++; }
      System.out.printf("MC again: %.0f bursts/h -> %d/%d removals%n", perHour, removed, N); }
    // offence ladder
    for (double eff : new double[]{0,1,2,3,4}) { int rg = Ladder.rung(eff,1,false); System.out.printf("offences %.0f -> rung %d : %s%n", eff, rg, rg <= 1 ? "kick" : Ladder.tempMinutes(rg,1,10,6,10080) + " min tempban"); }
  }
}
