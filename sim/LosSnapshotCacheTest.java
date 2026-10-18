import dev.meshac.LosSnapshotCache;
import static dev.meshac.LosSnapshotCache.Result.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class LosSnapshotCacheTest {
	private static void check(boolean b, String s) { if (!b) throw new AssertionError(s); }
	public static void main(String[] args) {
		LosSnapshotCache c = new LosSnapshotCache(8); AtomicInteger calls = new AtomicInteger();
		check(c.sample(0, 1, true, () -> { calls.incrementAndGet(); return false; }) == BLOCKED, "blocked result");
		check(c.sample(0, 1, true, () -> { throw new AssertionError("repeat clip"); }) == BLOCKED, "immutable historical snapshot");
		check(c.sample(1, 1, true, () -> true) == VISIBLE, "door change/new tick must recompute");
		for (int i=2;i<=8;i++) c.sample(1,i,true,()->false);
		check(c.size()==8, "cache bound");
		check(c.sample(1,9,true,()->{throw new AssertionError("over budget");})==UNKNOWN, "overflow is not blocked");
		check(c.sample(2,9,false,()->{throw new AssertionError("not selected");})==UNKNOWN, "not selected is unknown");
		boolean[] seen = new boolean[100];
		for(int t=0;t<25;t++) { int selected=0; for(int i=0;i<100;i++) if(LosSnapshotCache.selected(t,i,100,8)){selected++;seen[i]=true;} check(selected==8,"fair budget"); }
		for(boolean b:seen)check(b,"starved target");
		check(calls.get()==1,"initial clips");
		System.out.println("LosSnapshotCacheTest: cache invalidation, bound, unknown, fairness PASS");
	}
}
