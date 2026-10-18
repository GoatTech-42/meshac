import dev.meshac.veil.EspCore;
import dev.meshac.veil.RayBlocking;
import java.util.HashMap;
import java.util.Random;

/**
 * Pure bench of the anti-ESP ray cost (Veil.rayClear + Veil.visible): voxel walk, per-block decision through RayBlocking, and the 13 sample points x 3 eye origins loop with early exit.
 * Not in run-tests.sh (timing). Run:
 *   javac -d /dev/shm/vrb src/main/java/dev/meshac/veil/EspCore.java src/main/java/dev/meshac/veil/RayBlocking.java sim/VeilRayBench.java && nice -n 19 java -Xmx256m -cp /dev/shm/vrb VeilRayBench
 * Stand-ins: the world is a byte grid, the shape clip is a slab test against the block cube (a real VoxelShape.clip over a stair or slab is costlier), and the "chunk" lookup variant goes through a HashMap of 16x16 columns to mimic the indirection of getBlockState. Real numbers need the rig; treat these as order of magnitude.
 * Pair budget: Veil.RAYS_PER_TICK = 200 NEW cull tests per tick, each a visible() call of 1 to 39 rays.
 */
public class VeilRayBench {
	static final int W = 256, H = 128, D = 256;
	static final byte AIR = 0, STONE = 1, LEAVES = 2, FENCE = 3, SLAB = 4, GLASS = 5;
	static byte[] world = new byte[W * H * D];
	static HashMap<Long, byte[]> cols = new HashMap<>();
	static boolean viaMap;
	static int at(int x, int y, int z) { return (y * D + z) * W + x; }
	static byte get(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= W || y >= H || z >= D) return AIR;
		if (!viaMap) return world[at(x, y, z)];
		byte[] c = cols.get((long) (x >> 4) << 32 | (z >> 4));
		return c[((y * 16 + (z & 15)) * 16) + (x & 15)];
	}
	static void buildCols() {
		cols.clear();
		for (int cx = 0; cx < W / 16; cx++) for (int cz = 0; cz < D / 16; cz++) {
			byte[] c = new byte[16 * H * 16];
			for (int y = 0; y < H; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) c[(y * 16 + z) * 16 + x] = world[at(cx * 16 + x, y, cz * 16 + z)];
			cols.put((long) cx << 32 | cz, c);
		}
	}

	static long sink; static int blocksVisited;
	/** Same decision structure as Veil.rayClear. */
	static boolean rayClear(double fx, double fy, double fz, double tx, double ty, double tz) {
		double dx = tx - fx, dy = ty - fy, dz = tz - fz;
		int x = (int) Math.floor(fx), y = (int) Math.floor(fy), z = (int) Math.floor(fz);
		int ex = (int) Math.floor(tx), ey = (int) Math.floor(ty), ez = (int) Math.floor(tz);
		int sx = Integer.signum((int) Math.signum(dx)), sy = (int) Math.signum(dy), sz = (int) Math.signum(dz);
		double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
		double tMaxX = sx == 0 ? 1e30 : ((sx > 0 ? x + 1 - fx : fx - x) / ax), tMaxY = sy == 0 ? 1e30 : ((sy > 0 ? y + 1 - fy : fy - y) / ay), tMaxZ = sz == 0 ? 1e30 : ((sz > 0 ? z + 1 - fz : fz - z) / az);
		double tdx = sx == 0 ? 1e30 : 1 / ax, tdy = sy == 0 ? 1e30 : 1 / ay, tdz = sz == 0 ? 1e30 : 1 / az;
		for (int n = 0; n < 400; n++) {
			blocksVisited++;
			byte b = get(x, y, z);
			if (b != AIR) {
				boolean canOcclude = b != GLASS, foliage = b == LEAVES, gappy = b == FENCE;
				if (canOcclude && !foliage && !gappy) {
					// stand-in for shape.clip: slab test against the unit cube (slabs use half height)
					double top = b == SLAB ? y + 0.5 : y + 1;
					double t0 = 0, t1 = 1;
					double[] o = {fx, fy, fz}, d = {dx, dy, dz}, lo = {x, y, z}, hi = {x + 1, top, z + 1};
					boolean hit = true;
					for (int i = 0; i < 3 && hit; i++) {
						if (Math.abs(d[i]) < 1e-12) { if (o[i] < lo[i] || o[i] > hi[i]) hit = false; }
						else { double a = (lo[i] - o[i]) / d[i], c = (hi[i] - o[i]) / d[i]; if (a > c) { double t = a; a = c; c = t; } t0 = Math.max(t0, a); t1 = Math.min(t1, c); if (t0 > t1) hit = false; }
					}
					if (RayBlocking.blocks(false, true, false, false, false, hit)) return false;
				}
			}
			if (x == ex && y == ey && z == ez) break;
			if (tMaxX < tMaxY && tMaxX < tMaxZ) { x += sx; tMaxX += tdx; } else if (tMaxY < tMaxZ) { y += sy; tMaxY += tdy; } else { z += sz; tMaxZ += tdz; }
		}
		return true;
	}
	static final double[] BUF = new double[EspCore.SAMPLE_DOUBLES];
	static int rays;
	static boolean visible(double ex, double ey, double ez, double vx, double vz, double mx, double my, double mz) {
		int np = EspCore.samplePointsInto(BUF, mx - 0.3, my, mz - 0.3, mx + 0.3, my + 1.8, mz + 0.3);
		double[][] eyes = {{ex, ey, ez}, {ex + vx * 6, ey, ez + vz * 6}, {ex, ey - 0.4, ez}};
		for (double[] o : eyes) for (int i = 0; i < np; i += 3) { rays++; if (rayClear(o[0], o[1], o[2], BUF[i], BUF[i + 1], BUF[i + 2])) return true; }
		return false;
	}

	static void terrain(Random r, int treeEvery, boolean tunnels, boolean base) {
		java.util.Arrays.fill(world, AIR);
		int[] hgt = new int[W * D];
		for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) hgt[x * D + z] = (int) (60 + 8 * Math.sin(x / 23.0) * Math.cos(z / 31.0) + 4 * Math.sin((x + z) / 9.0));
		for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) for (int y = 0; y <= hgt[x * D + z]; y++) world[at(x, y, z)] = STONE;
		if (tunnels) for (int t = 0; t < 120; t++) { int x = r.nextInt(W), y = 20 + r.nextInt(35), z = r.nextInt(D), dx = r.nextInt(3) - 1, dz = r.nextInt(3) - 1; for (int s = 0; s < 80; s++) { for (int a = 0; a < 2; a++) for (int b = 0; b < 3; b++) { int px = x + a, py = y + b, pz = z; if (px >= 0 && px < W && pz >= 0 && pz < D) world[at(px, py, pz)] = AIR; } x += dx; z += dz; if (r.nextInt(6) == 0) { dx = r.nextInt(3) - 1; dz = r.nextInt(3) - 1; } if (x < 1 || x >= W - 2 || z < 1 || z >= D - 2) break; } }
		if (treeEvery > 0) for (int x = 3; x < W - 3; x += 2) for (int z = 3; z < D - 3; z += 2) if (r.nextInt(treeEvery) == 0) { int h = hgt[x * D + z]; for (int y = 1; y <= 5; y++) world[at(x, h + y, z)] = STONE; for (int a = -2; a <= 2; a++) for (int b = -2; b <= 2; b++) for (int y = 4; y <= 7; y++) if (world[at(x + a, h + y, z + b)] == AIR) world[at(x + a, h + y, z + b)] = LEAVES; }
		if (base) for (int bx = 20; bx < W - 20; bx += 40) for (int bz = 20; bz < D - 20; bz += 40) { int h = hgt[bx * D + bz] + 1; for (int i = 0; i < 20; i++) { for (int y = 0; y < 4; y++) { world[at(bx + i, h + y, bz)] = y == 3 ? SLAB : STONE; world[at(bx + i, h + y, bz + 15)] = STONE; world[at(bx, h + y, bz + i % 16)] = STONE; } world[at(bx + i, h, bz + 18)] = FENCE; world[at(bx + i, h + 1, bz + 18)] = FENCE; world[at(bx + i, h + 3, bz + 4)] = GLASS; } }
	}

	/** One tick: 200 pairs from one viewer, entities placed 9..128 away at ground level or underground. Returns ms. */
	static double tick(Random r, boolean viewerUnder, boolean targetsUnder) {
		double ex = 128 + r.nextGaussian() * 20, ez = 128 + r.nextGaussian() * 20, ey = viewerUnder ? 30 : 70;
		long t0 = System.nanoTime();
		for (int i = 0; i < 200; i++) {
			double ang = r.nextDouble() * Math.PI * 2, dist = 9 + r.nextDouble() * 100;
			double mx = Math.max(2, Math.min(W - 3, ex + Math.cos(ang) * dist)), mz = Math.max(2, Math.min(D - 3, ez + Math.sin(ang) * dist));
			int gy = 40; while (gy < H - 4 && get((int) mx, gy, (int) mz) != AIR) gy++;
			double my = targetsUnder ? 25 + r.nextInt(20) : gy;
			sink += visible(ex, ey + 1.62, ez, 0.2, 0.1, mx, my, mz) ? 1 : 0;
		}
		return (System.nanoTime() - t0) / 1e6;
	}

	public static void main(String[] a) {
		Random r = new Random(5);
		String[] names = {"open field, sparse trees (mostly visible)", "forest + bases, surface viewer (mixed)", "viewer and targets inside rock/tunnels (blocked at once, 39 rays each)", "surface viewer, targets underground (blocked after a long walk, worst case)"};
		for (int mode = 0; mode < 2; mode++) {
			viaMap = mode == 1;
			System.out.println(viaMap ? "== block lookup through a chunk-column HashMap (pessimistic)" : "== block lookup straight from an array (floor)");
			for (int sc = 0; sc < 4; sc++) {
				terrain(new Random(7), sc == 0 ? 60 : sc == 1 ? 8 : 0, sc >= 2, sc == 1);
				buildCols();
				double[] ms = new double[400]; rays = 0; blocksVisited = 0;
				for (int w = 0; w < 100; w++) tick(r, sc == 2, sc >= 2); // warm up
				rays = 0; blocksVisited = 0;
				for (int t = 0; t < ms.length; t++) ms[t] = tick(r, sc == 2, sc >= 2);
				java.util.Arrays.sort(ms);
				double sum = 0; for (double m : ms) sum += m;
				System.out.printf("%-80s mean %.2f ms/tick  p95 %.2f  max %.2f  (%.1f rays/pair, %.0f blocks/ray)%n", names[sc], sum / ms.length, ms[(int) (ms.length * 0.95)], ms[ms.length - 1], rays / (400.0 * 200), blocksVisited / (double) rays);
			}
		}
		System.out.println("sink " + sink);
	}
}
