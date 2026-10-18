import dev.meshac.BounceTracker;

// Rig replays: the candidate8 bounce series the ilhsmc rig recorded (slime 10-fall, slime 25-fall, bed 10-fall).
// Replayed through the tracker the way Movement drives it (land at ground contact with the pad's restitution,
// bouncing() queried per packet); the legit series must produce no uncovered rise. Setback snaps (the server
// teleporting the player back mid-series) get the same grace the live check gives them.
public class BounceFixtureTest {
	private static int failed = 0;
	private static void check(boolean ok, String what) { if (!ok) { System.out.println("FAIL " + what); failed++; } }

	// rig recording on candidate8 (Bedrock client through Geyser), fp-bounce-slime-10-c371c9a4.json: [seconds, y, dy] triples
	private static final float[] SLIME_10 = {0.101f,79.85489329934836f,-0.7811331932098841f,0.352f,74.40617885469534f,-1.082713105363373f,0.534f,74.40617885469534f,-1.302977000006099f,0.715f,74.62904809940291f,0.16477328182606651f,0.921f,77.29872831619902f,-0.07544406518948656f,1.108f,77.39562672447423f,-0.4448259643949201f,1.338f,77.39562672447423f,-0.7146101656984427f,1.561f,75.27450222863567f,-0.9634505424243047f,1.815f,75.27450222863567f,-1.2475135649730786f,2.045f,73.30075087759798f,-0.0784000015258789f,2.286f,73.30075087759798f,-0.44749789698341763f,2.505f,72.532274675185f,-0.7170746714356033f,2.685f,71.51661730828683f,0.16477328182606651f,2.922f,71.51661730828683f,-0.15233518685055708f,3.115f,72.37889772202342f,-0.5143294551172826f,3.313f,71.8255654114511f,0.24813599859094576f,3.523f,72.29714122939778f,-0.07544406518948656f,3.699f,72.29714122939778f,-0.3739040364667221f,3.91f,71.40741557324442f,0.33319999363422365f,4.11f,72.18873205466316f,-0.07544406518948656f,4.321f,71.0f,-0.30153472366278034f,4.492f,71.84456534309474f,0.12138813952494364f,4.695f,71.94788142716658f,-0.19235226384154985f,4.875f,71.00755269517389f,0.3262266023779985f,5.065f,71.00755269517389f,-0.003415773053661954f};
	private static final double SLIME_10_R = 1.0;
	// rig recording on candidate8 (Bedrock client through Geyser), fp-bounce-slime-25-45f9135b.json: [seconds, y, dy] triples
	private static final float[] SLIME_25 = {0.058f,91.936654946153f,-0.7811331932098841f,0.268f,86.33293275542411f,-0.9657237242233685f,0.457f,83.99840417860725f,-1.1950697113837683f,0.651f,80.09054135074086f,-1.406611164032995f,0.839f,74.16649117765083f,-1.6017302796669999f,1.0f,71.0f,-0.0784000015258789f,1.158f,71.0f,-0.0784000015258789f,1.328f,71.0f,-0.0784000015258789f,1.514f,71.0f,-0.0784000015258789f,1.686f,71.0f,-0.0784000015258789f,1.852f,71.0f,-0.0784000015258789f,2.005f,71.0f,-0.0784000015258789f,2.183f,71.0f,-0.0784000015258789f,2.338f,71.0f,-0.0784000015258789f,2.489f,71.0f,-0.0784000015258789f,2.649f,71.0f,-0.0784000015258789f,2.818f,71.0f,-0.0784000015258789f,2.978f,71.0f,-0.0784000015258789f,3.134f,71.0f,-0.0784000015258789f,3.304f,71.0f,-0.0784000015258789f,3.482f,71.0f,-0.0784000015258789f,3.64f,71.0f,-0.0784000015258789f,3.808f,71.0f,-0.0784000015258789f,3.971f,71.0f,-0.0784000015258789f,4.127f,71.0f,-0.0784000015258789f,4.295f,71.0f,-0.0784000015258789f,4.449f,71.0f,-0.0784000015258789f,4.623f,71.0f,-0.0784000015258789f,4.79f,71.0f,-0.0784000015258789f,4.949f,71.0f,-0.0784000015258789f};
	private static final double SLIME_25_R = 1.0;
	// rig recording on candidate8 (Bedrock client through Geyser), fp-bounce-bed-10-bd7638e2.json: [seconds, y, dy] triples
	private static final float[] BED_10 = {0.044f,76.936654946153f,-0.7170746714356033f,0.202f,74.40617885469534f,-0.9054323524772837f,0.361f,71.33293275542411f,0.7473140143357201f,0.525f,72.04235266478307f,0.47283864393599145f,0.694f,74.41552519076241f,0.214504606035065f,0.861f,74.76184938274979f,-0.10646459327446993f,1.032f,74.49479991832867f,-0.3307309982368606f,1.205f,74.2373215825088f,-0.5418087567806779f,1.383f,73.0312146661953f,-0.8040640124270239f,1.551f,71.48697557730534f,0.33705662621541743f,1.712f,72.22724322358617f,0.006573496883688566f,1.873f,72.07481918990737f,-0.22434044587276797f,2.033f,71.16334449529822f,-0.44167481417336996f,2.196f,70.87914141098884f,0.0985537209669463f,2.353f,71.32741342808522f,-0.13776938975266848f,2.51f,71.05774138229589f,-0.43139093588087735f,2.674f,70.87271579713251f,0.10316800528102361f,2.863f,70.86018305126251f,-0.1334264620169576f,3.027f,70.5625f,-0.0784000015258789f,3.189f,70.5625f,-0.0784000015258789f,3.351f,70.5625f,-0.0784000015258789f,3.505f,70.5625f,-0.0784000015258789f,3.675f,70.5625f,-0.0784000015258789f,3.827f,70.5625f,-0.0784000015258789f,3.979f,70.5625f,-0.0784000015258789f,4.132f,70.5625f,-0.0784000015258789f,4.289f,70.5625f,-0.0784000015258789f,4.441f,70.5625f,-0.0784000015258789f,4.595f,70.5625f,-0.0784000015258789f,4.764f,70.5625f,-0.0784000015258789f,4.922f,70.5625f,-0.0784000015258789f};
	private static final double BED_10_R = 0.66;

	private static int[] replay(float[] s, double restitution) {
		BounceTracker t = new BounceTracker();
		double prevY = s[1], peakY = s[1], graceUntil = -1;
		boolean falling = false, landed = false, armedOnce = false;
		int uncovered = 0;
		for (int i = 3; i < s.length; i += 3) {
			double ms = s[i] * 1000, y = s[i + 1];
			if (ms < graceUntil) { prevY = y; continue; } // own setback: the live check is in grace
			if (y - prevY > 0.5 && !landed) { graceUntil = ms + 1000; peakY = Math.max(peakY, y); prevY = y; continue; } // setback snap mid-fall
			if (y - prevY > 0.5) { graceUntil = ms + 1000; prevY = y; continue; } // setback snap during the bounce
			if (y < prevY - 1e-6) falling = true;
			if (falling && y >= prevY - 1e-6) { // ground contact: the fall that ended here
				t.land(prevY, Math.max(0, peakY - prevY), restitution, (long) ms);
				armedOnce |= t.armed();
				landed = true; falling = false; peakY = prevY;
			}
			if (landed && y > prevY + 1e-6 && !t.bouncing(y, (long) ms)) uncovered++;
			if (landed) t.bouncing(y, (long) ms); // keep the cap accounting honest on every packet
			peakY = Math.max(peakY, y);
			prevY = y;
		}
		return new int[] {uncovered, armedOnce ? 1 : 0};
	}

	public static void main(String[] a) {
		int[] r = replay(SLIME_10, SLIME_10_R);
		check(r[1] == 1, "slime-10: tracker armed on the landing (fall 8.85)");
		check(r[0] == 0, "slime-10: every legit rise covered, " + r[0] + " uncovered");
		r = replay(SLIME_25, SLIME_25_R);
		check(r[1] == 1, "slime-25: tracker armed on the landing (fall 20.94)");
		check(r[0] == 0, "slime-25: no uncovered rise (setback killed the bounce chain, nothing to cover)");
		r = replay(BED_10, BED_10_R);
		check(r[1] == 1, "bed-10: tracker armed on the landing (fall 5.61)");
		check(r[0] == 0, "bed-10: decay chain into the microhop band covered, " + r[0] + " uncovered");

		// negative control: the same slime-10 series landing on stone (restitution 0) must NOT be covered
		r = replay(SLIME_10, 0.0);
		check(r[1] == 0, "stone landing never arms");
		check(r[0] > 0, "stone landing leaves the rises to flag");

		if (failed > 0) throw new AssertionError(failed + " failures");
		System.out.println("BounceFixtureTest ok");
	}
}
