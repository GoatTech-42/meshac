package dev.meshac;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Posts a case to a Discord webhook if one is configured. Fire and forget; a failed post never affects play. */
public final class Discord {
	private static final HttpClient HTTP = HttpClient.newHttpClient();
	public static void post(Cases.Case c) {
		String url = Config.get().discordWebhook;
		if (url == null || url.isBlank()) return;
		String until = c.until > 0 ? " until " + new java.util.Date(c.until) : "";
		String ev = String.join("\n", c.evidence.subList(Math.max(0, c.evidence.size() - 6), c.evidence.size()));
		String text = "**" + c.action + "** " + c.player + until + " | case " + c.id + "\n" + c.reason + "\n```\n" + ev + "\n```";
		String json = "{\"content\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
		HTTP.sendAsync(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.discarding());
	}
}
