package dev.meshac;

/** Descriptive features 1, 7 and 10. No score, threshold, verdict or side effects. */
public final class CombatFeatures {
	private static final int CAPACITY = 64;
	private final double[] lateral = new double[CAPACITY], vertical = new double[CAPACITY], velocity = new double[CAPACITY], charge = new double[CAPACITY], gap = new double[CAPACITY], nominalSlack = new double[CAPACITY];
	private int n, cursor, target = Integer.MIN_VALUE;
	private String weapon;
	private long lastNs;
	public record Summary(int n, double lateralVariance, double verticalVariance, double lateralLag1, boolean lag1Defined, double fullChargeShare, double meanGapMs, double meanNominalSlackMs, double trackingCorrelation, boolean trackingDefined, int movingSamples) {}

	/** Clear on fight gap, target or weapon change. Times are server handling times. */
	public Summary observe(long ns, int entity, String heldWeapon, double lat, double vert, double targetLateralVelocity, double cooldown, double nominalFullChargeMs) {
		if (!Double.isFinite(lat) || !Double.isFinite(vert) || !Double.isFinite(targetLateralVelocity) || !Double.isFinite(cooldown) || !Double.isFinite(nominalFullChargeMs) || nominalFullChargeMs <= 0) return null;
		boolean reset = lastNs == 0 || ns <= lastNs || ns - lastNs > 3_000_000_000L || target != entity || !java.util.Objects.equals(weapon, heldWeapon);
		if (reset) { n = 0; cursor = 0; lastNs = 0; }
		double gapMs = lastNs == 0 ? Double.NaN : (ns - lastNs) / 1e6;
		int i = cursor; lateral[i] = lat; vertical[i] = vert; velocity[i] = targetLateralVelocity; charge[i] = cooldown;
		gap[i] = gapMs; nominalSlack[i] = gapMs - nominalFullChargeMs;
		cursor = (cursor + 1) % CAPACITY; n = Math.min(CAPACITY, n + 1); lastNs = ns; target = entity; weapon = heldWeapon;
		return summary();
	}
	private int index(int i) { return Math.floorMod(cursor - n + i, CAPACITY); }
	private Summary summary() {
		double ml=0,mv=0,mt=0,full=0,g=0,slack=0; int gaps=0,moving=0;
		for(int j=0;j<n;j++){int i=index(j); ml+=lateral[i];mv+=vertical[i];mt+=velocity[i];full+=charge[i]>=1?1:0;if(Math.abs(velocity[i])>0.001)moving++;if(Double.isFinite(gap[i])){g+=gap[i];slack+=nominalSlack[i];gaps++;}}
		ml/=n;mv/=n;mt/=n;double vl=0,vv=0,vt=0,cov=0;
		for(int j=0;j<n;j++){int i=index(j);double a=lateral[i]-ml,b=velocity[i]-mt;vl+=a*a;vv+=(vertical[i]-mv)*(vertical[i]-mv);vt+=b*b;cov+=a*b;}
		boolean tracking=n>=8 && moving>=8 && vl>1e-12 && vt>1e-12;
		double lagCov=0,lagA=0,lagB=0;
		for(int j=1;j<n;j++){double a=lateral[index(j-1)]-ml,b=lateral[index(j)]-ml;lagCov+=a*b;lagA+=a*a;lagB+=b*b;}
		boolean lag=n>=8 && lagA>1e-12 && lagB>1e-12;
		return new Summary(n,vl/n,vv/n,lag?lagCov/Math.sqrt(lagA*lagB):0,lag,full/n,gaps>0?g/gaps:0,gaps>0?slack/gaps:0,tracking?cov/Math.sqrt(vl*vt):0,tracking,moving);
	}
}
