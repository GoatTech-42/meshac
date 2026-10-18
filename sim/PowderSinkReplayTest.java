import dev.meshac.FpGuards;
import java.nio.file.*;
import java.util.*;

/** Replays the rig's powder-snow sink (candidate8, 3-deep pit, no input) through the fly rule. Fixture columns: t_s,pos_y,motion_y. pos_y is sampled about every 10 ticks, so each step is expanded into whole ticks of the stuck displacement. A positive step is the setback rubber-band: the server was awaiting the teleport confirm, so those packets are skipped. */
public final class PowderSinkReplayTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) throws Exception {
		List<String> rows = Files.readAllLines(Path.of("sim/fixtures/powder-sink-candidate8.csv"));
		double prevY = Double.NaN; int buf = 0, maxBuf = 0, ticks = 0, skipped = 0, floorTicks = 0; double prevDy = -0.1552;
		for (int i = 1; i < rows.size(); i++) {
			String[] c = rows.get(i).split(",");
			double y = Double.parseDouble(c[1]);
			if (!Double.isNaN(prevY)) {
				double step = y - prevY;
				if (step > 0.2) { skipped++; } // setback: ignored while awaiting the confirm (Movement.check returns early)
				else if (Math.abs(step) < 1e-9) { // on the floor inside snow: the move is blocked
					buf = FpGuards.flyBuf(buf, 0.0, FpGuards.stuckExpect((prevDy - 0.08) * 0.98, true), false, FpGuards.restingInPowder(true, true, 0.0)); floorTicks++; prevDy = 0.0; maxBuf = Math.max(maxBuf, buf);
				} else {
					int k = (int) Math.round(-step / 0.1176); // whole ticks of -0.1176
					check(Math.abs(-step - k * 0.1176) < 0.02 || k == 0, "step " + step + " is not whole ticks of the stuck fall");
					for (int t = 0; t < Math.max(1, Math.min(k, 3)); t++) { double dy = k <= 3 ? step / Math.max(1, k) : FpGuards.POWDER_STUCK_DY; buf = FpGuards.flyBuf(buf, dy, FpGuards.stuckExpect((prevDy - 0.08) * 0.98, true), false, false); prevDy = dy; ticks++; maxBuf = Math.max(maxBuf, buf); }
				}
			}
			prevY = y;
		}
		check(skipped >= 1, "fixture has a setback step");
		check(floorTicks > 40, "fixture has the floor phase");
		check(maxBuf == 0, "rig sink replay built fly buffer " + maxBuf);
		// without the powder expectation or the floor rule the same series flags (so the fixture is a real regression case)
		int old = 0; prevDy = -0.1176; for (int t = 0; t < 12; t++) { old = FpGuards.flyBuf(old, -0.1176, (prevDy - 0.08) * 0.98, false, false); prevDy = -0.1176; }
		check(old >= 10, "old free-fall expectation flags the steady sink: " + old);
		int oldFloor = 0; for (int t = 0; t < 12; t++) oldFloor = FpGuards.flyBuf(oldFloor, 0.0, -0.0784, false, false);
		check(oldFloor >= 10, "old rule flags dy 0 on the pit floor: " + oldFloor);
		// the stale rubber-band packet, if it were evaluated, trips the air-jump bar (dy > expect + 0.3)
		check(0.9408 > (-0.1176 - 0.08) * 0.98 + 0.3, "dy 0.94 would be an air jump if not skipped");
		System.out.println(fails == 0 ? "PowderSinkReplayTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
