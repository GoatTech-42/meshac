import dev.meshac.LosSnapshotCache;

/** Synthetic four-ray supplier. Reports JVM timing, NOT Minecraft collision cost. */
public final class LosSnapshotBenchmark {
	static volatile long sink;
	static long clips;
	static boolean fourRays() {
		long x=sink;
		for(int r=0;r<4;r++){clips++;for(int i=0;i<24;i++) x=(x*6364136223846793005L+1442695040888963407L)^(x>>>13);}
		sink=x;return false;
	}
	static void run(boolean bounded,int ticks) {
		LosSnapshotCache[] c=new LosSnapshotCache[20];for(int p=0;p<20;p++)c[p]=new LosSnapshotCache(8);
		clips=0;long start=System.nanoTime();long unknown=0;
		for(int tick=0;tick<ticks;tick++)for(int p=0;p<20;p++)for(int e=0;e<100;e++){
			if(!bounded)fourRays();
			else {
				boolean selected=LosSnapshotCache.selected(tick,e,100,8);
				if(c[p].sample(tick,e,selected,LosSnapshotBenchmark::fourRays)==LosSnapshotCache.Result.UNKNOWN)unknown++;
			}
		}
		long ns=System.nanoTime()-start;
		System.out.printf("mode=%s ticks=%d players=20 entities=100 clips=%d clipsPerTick=%.0f msPerTick=%.4f unknown=%d%n",bounded?"bounded":"baseline",ticks,clips,(double)clips/ticks,ns/1e6/ticks,unknown);
	}
	public static void main(String[] args){run(false,100);run(true,100);for(int i=0;i<3;i++){run(false,2000);run(true,2000);}}
}
