package dev.meshac;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Posts to a Discord webhook if one is configured: every case (kick, tempban, ban) and the first hold of a heat build-up. Fire and forget; a failed post never affects play. */
public final class Discord {
	private static final HttpClient HTTP = HttpClient.newHttpClient();

	public static void post(Cases.Case c) {
		String until = c.until > 0 ? " until " + new java.util.Date(c.until) : "";
		String ev = String.join("\n", c.evidence.subList(Math.max(0, c.evidence.size() - 6), c.evidence.size()));
		send("**" + c.action + "** " + c.player + until + " | case " + c.id + "\n" + c.reason + "\n```\n" + ev + "\n```");
	}
	/** A player's heat reached the hold step. Below that, strikes are too noisy for a channel. */
	public static void hold(String player, String check, String detail) {
		if (Config.get().discordOnHold) send("**flag** " + player + " | " + check + "\n" + detail);
	}
	private static void send(String text) {
		String url = Config.get().discordWebhook;
		if (url == null || url.isBlank()) return;
		JsonObject o = new JsonObject();
		o.addProperty("content", text.length() > 1900 ? text.substring(0, 1900) : text);
		HTTP.sendAsync(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(o.toString())).build(), HttpResponse.BodyHandlers.discarding());
	}
}
