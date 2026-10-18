import dev.meshac.SafeSwap;

public final class SafeSwapTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		check(SafeSwap.parse(() -> "ok").orElse("prior").equals("ok"), "good parse is returned");
		check(SafeSwap.parse(() -> { throw new IllegalStateException("bad json"); }).orElse("prior").equals("prior"), "a parse error keeps the prior value");
		check(SafeSwap.parse(() -> (String) null).isEmpty(), "a null result is rejected");
		check(SafeSwap.parse(() -> Integer.parseInt("x")).isEmpty(), "a number format error is rejected");
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("SafeSwapTest ok");
	}
}
