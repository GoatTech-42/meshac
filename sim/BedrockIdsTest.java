import dev.meshac.BedrockIds;
import java.util.UUID;

public class BedrockIdsTest {
	static int fails = 0;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	static long clock;
	public static void main(String[] a) {
		UUID unlinked = new UUID(0L, 2535412345L);
		UUID linked = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"); // a normal Java uuid that Floodgate says is Bedrock
		UUID java = UUID.fromString("11111111-2222-4333-8444-555555555555");
		UUID offline = UUID.nameUUIDFromBytes("OfflinePlayer:.Steve".getBytes());
		String api = "FakeFloodgateApi";
		// ---- Floodgate not installed: zero-high rule only, and the class is looked up once ----
		int[] loads = {0};
		BedrockIds none = new BedrockIds(() -> { loads[0]++; return BedrockIds.loadFloodgate("no.such.Api"); }, () -> clock);
		check(none.isBedrock(unlinked), "unlinked uuid without Floodgate");
		check(!none.isBedrock(linked) && !none.isBedrock(java), "no Floodgate: normal uuids are Java");
		for (int i = 0; i < 100; i++) none.isBedrock(java);
		check(loads[0] == 1 && none.floodgateAbsent(), "class lookup happens once, got " + loads[0]);
		check(none.size() == 0, "nothing cached when Floodgate is absent");
		java.util.List<String> lines = new java.util.ArrayList<>(); BedrockIds none2 = new BedrockIds(() -> BedrockIds.loadFloodgate("no.such.Api"), () -> clock); none2.onLog(lines::add); none2.isBedrock(java); none2.isBedrock(java);
		check(lines.size() == 1 && lines.get(0).contains("not found"), "one line when the API is missing, got " + lines);
		// ---- Floodgate there, not started: null instance, retried later ----
		clock = 1000; FakeFloodgateApi.instance = null; FakeFloodgateApi.players.add(linked);
		int[] l2 = {0};
		java.util.List<String> blog = new java.util.ArrayList<>();
		BedrockIds b = new BedrockIds(() -> { l2[0]++; return BedrockIds.loadFloodgate(api); }, () -> clock);
		b.onLog(blog::add);
		check(!b.isBedrock(linked), "Floodgate not started: not Bedrock yet, no crash");
		check(!b.isBedrock(linked) && l2[0] == 1, "not retried within " + BedrockIds.NOT_READY_MS + " ms");
		FakeFloodgateApi inst = new FakeFloodgateApi(); FakeFloodgateApi.instance = inst;
		clock += BedrockIds.NOT_READY_MS + 1;
		check(b.isBedrock(linked), "after Floodgate started: the linked account is Bedrock");
		check(blog.size() == 2 && blog.get(0).contains("found") && blog.get(1).contains(linked.toString()), "log: api found, then the linked uuid once: " + blog);
		// ---- linked account is found, a Java client is not, and answers are cached ----
		int c0 = inst.calls;
		for (int i = 0; i < 1000; i++) check(b.isBedrock(linked), "linked stays Bedrock");
		check(inst.calls == c0, "positive answer cached, no API call per packet");
		check(!b.isBedrock(java) && !b.isBedrock(offline), "Java and offline-mode uuids are not Bedrock");
		int c1 = inst.calls; for (int i = 0; i < 1000; i++) b.isBedrock(java);
		check(inst.calls == c1, "negative answer cached within its time to live");
		// ---- a late registration: the no expires and is asked again ----
		UUID late = UUID.fromString("22222222-3333-4444-8555-666666666666");
		check(!b.isBedrock(late), "not registered yet");
		FakeFloodgateApi.players.add(late);
		check(!b.isBedrock(late), "still the cached no inside the time to live");
		clock += BedrockIds.NEG_TTL_MS + 1;
		check(b.isBedrock(late), "asked again after the time to live, now Bedrock");
		// ---- leave and rejoin ----
		b.forget(linked); FakeFloodgateApi.players.remove(linked);
		check(!b.isBedrock(linked), "after leaving and unlinking the old answer is gone");
		// ---- the zero-high rule never asks Floodgate ----
		int c2 = inst.calls; check(b.isBedrock(unlinked) && inst.calls == c2, "zero-high uuid: no API call");
		// ---- the API throws: false, backoff, then recovers ----
		FakeFloodgateApi.throwing = true; UUID x = UUID.fromString("33333333-4444-4555-8666-777777777777"); FakeFloodgateApi.players.add(x);
		check(!b.isBedrock(x), "API error: treated as not Bedrock (Java checks apply), no exception out");
		int c3 = inst.calls; for (int i = 0; i < 100; i++) b.isBedrock(x);
		check(inst.calls == c3, "no call storm during the error backoff");
		FakeFloodgateApi.throwing = false; clock += BedrockIds.ERROR_BACKOFF_MS + 1;
		check(b.isBedrock(x), "recovers after the backoff");
		// ---- cache stays bounded ----
		for (int i = 0; i < 6000; i++) b.isBedrock(new UUID(i + 1L, i));
		check(b.size() <= 4097, "cache bounded, size " + b.size());
		// ---- a Java client with a name that looks like a Floodgate prefix is still Java ----
		check(!b.isBedrock(offline), "the name is never consulted");
		// ---- API with a different shape: given up on, uuid rule only ----
		BedrockIds odd = new BedrockIds(() -> BedrockIds.loadFloodgate("java.lang.String"), () -> clock);
		check(!odd.isBedrock(java) && odd.floodgateAbsent() && odd.isBedrock(unlinked), "unknown API shape: uuid rule only");
		// ================= edge cases (Floodgate hook review) =================
		FakeFloodgateApi.players.clear(); FakeFloodgateApi.throwing = false;
		// ---- names never matter: prefix variations all give normal offline UUIDs and are Java ----
		{
			FakeFloodgateApi i0 = new FakeFloodgateApi(); FakeFloodgateApi.instance = i0; clock = 5_000_000;
			BedrockIds n = new BedrockIds(() -> BedrockIds.loadFloodgate(api), () -> clock);
			String[] names = {".Steve", "*Steve", "_Steve", "+Steve", ".", "", "Steve", ".Steve_Smith", "..Steve", "BE_Steve", ".RulerofClubs"};
			for (String nm : names) {
				UUID u = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nm).getBytes());
				check(u.getMostSignificantBits() != 0L, "offline uuid for \"" + nm + "\" can never have a zero high half");
				check(!n.isBedrock(u), "a Java client named \"" + nm + "\" is not skipped");
			}
			for (int i = 0; i < 20000; i++) check(UUID.nameUUIDFromBytes(("OfflinePlayer:." + i).getBytes()).getMostSignificantBits() != 0L, "version bits keep offline uuids out of rule 1");
			// same name, different uuid: only the registered uuid is Bedrock
			UUID realLinked = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), spoofer = UUID.nameUUIDFromBytes("OfflinePlayer:.Steve".getBytes());
			FakeFloodgateApi.players.add(realLinked);
			check(n.isBedrock(realLinked) && !n.isBedrock(spoofer), "exemption follows the uuid, not the name");
			// unlinked Bedrock: zero-high uuid, Floodgate never asked
			int k = i0.calls; check(n.isBedrock(new UUID(0L, 987654321L)) && i0.calls == k, "unlinked Bedrock needs no API call");
		}
		// ---- linked vs unlinked, linked after a while, unlinked-then-linked: Floodgate decides ----
		{
			FakeFloodgateApi i1 = new FakeFloodgateApi(); FakeFloodgateApi.instance = i1; clock = 6_000_000; FakeFloodgateApi.players.clear();
			BedrockIds n = new BedrockIds(() -> BedrockIds.loadFloodgate(api), () -> clock);
			UUID acct = UUID.fromString("0a0a0a0a-1111-4222-8333-444444444444");
			check(!n.isBedrock(acct), "the same account on a Java client: Java");
			FakeFloodgateApi.players.add(acct); clock += BedrockIds.NEG_TTL_MS + 1;
			check(n.isBedrock(acct), "now connected through Geyser and linked: Bedrock");
			// ---- a yes expires: Floodgate dropped the player (it keeps a leaver for a short while), the next Java session is not skipped for good ----
			FakeFloodgateApi.players.remove(acct);
			clock += BedrockIds.POS_TTL_MS - 10;
			check(n.isBedrock(acct), "inside the positive time to live the old yes holds (Floodgate's own pending-remove window)");
			clock += 20;
			check(!n.isBedrock(acct), "after the positive time to live the account is Java again");
			// join forgets: a new session starts clean
			FakeFloodgateApi.players.add(acct); clock += BedrockIds.NEG_TTL_MS + 1; check(n.isBedrock(acct), "linked again");
			FakeFloodgateApi.players.remove(acct); n.forget(acct); check(!n.isBedrock(acct), "forget on join/leave drops the yes at once");
		}
		// ---- Floodgate reload: a new instance that has only seen new joins ----
		{
			FakeFloodgateApi old = new FakeFloodgateApi(); old.useShared = false; FakeFloodgateApi.instance = old; clock = 7_000_000;
			BedrockIds n = new BedrockIds(() -> BedrockIds.loadFloodgate(api), () -> clock);
			UUID ua = UUID.fromString("0b0b0b0b-1111-4222-8333-444444444444"), joined = UUID.fromString("0c0c0c0c-1111-4222-8333-444444444444");
			old.mine.add(ua); check(n.isBedrock(ua), "before the reload");
			FakeFloodgateApi fresh = new FakeFloodgateApi(); fresh.useShared = false; fresh.mine.add(joined); FakeFloodgateApi.instance = fresh; // reload: the old instance is gone
			check(n.isBedrock(joined), "a player who joins after the reload is found through the NEW instance (a kept reference would say no)");
			check(n.isBedrock(ua), "a yes known before the reload holds inside its time to live");
			// reload window: no instance at all
			FakeFloodgateApi.instance = null; clock += BedrockIds.POS_TTL_MS + 1;
			check(n.isBedrock(ua) && n.isBedrock(joined), "during the reload a known yes stays yes even after it expired (no flip to Java)");
			UUID midJoin = UUID.fromString("0d0d0d0d-1111-4222-8333-444444444444");
			check(!n.isBedrock(midJoin), "a player with no known answer while Floodgate is down is Java, no exception");
			FakeFloodgateApi back = new FakeFloodgateApi(); back.useShared = false; back.mine.add(midJoin); back.mine.add(ua); FakeFloodgateApi.instance = back;
			check(n.isBedrock(midJoin), "the moment the instance is back the mid-reload joiner is answered (the not-ready answer was not cached as a no)");
			check(n.isBedrock(ua), "the earlier yes is confirmed again by the new instance");
			check(!n.floodgateAbsent(), "a reload is not treated as Floodgate missing");
		}
		// ---- the log says "treated as Bedrock" once per player, not on every refresh ----
		{
			FakeFloodgateApi i2 = new FakeFloodgateApi(); FakeFloodgateApi.instance = i2; clock = 8_000_000; FakeFloodgateApi.players.clear();
			UUID ua = UUID.fromString("0e0e0e0e-1111-4222-8333-444444444444"); FakeFloodgateApi.players.add(ua);
			java.util.List<String> lg = new java.util.ArrayList<>();
			BedrockIds n = new BedrockIds(() -> BedrockIds.loadFloodgate(api), () -> clock); n.onLog(lg::add);
			for (int i = 0; i < 10; i++) { n.isBedrock(ua); clock += BedrockIds.POS_TTL_MS + 1; }
			check(lg.size() == 2, "api found + one linked line across ten refreshes, got " + lg);
		}
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("BedrockIdsTest ok");
	}
}
