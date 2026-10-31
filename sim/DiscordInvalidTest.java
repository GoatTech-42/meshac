package dev.meshac;
/** Real Discord and Gson. All URLs are rejected before any network request. */
public class DiscordInvalidTest {
 public static void main(String[] args) throws Exception {
  int checks=0;
  for(String url:new String[]{null,""," ",":bad","file:///tmp/webhook","https://", "relative-path", ":private-token-test"}) {
   Config.cur.discordWebhook=url;
   try { Discord.hold("test","fly","scripted"); }
   catch(RuntimeException error) { throw new AssertionError("optional webhook must not interrupt hold callback",error); }
   checks++;
   try { Discord.post(new Cases.Case()); }
   catch(RuntimeException error) { throw new AssertionError("optional webhook must not interrupt removal callback",error); }
   checks++;
  }
  if(Meshac.LOG.lines.size()!=10) throw new AssertionError("each invalid nonblank URL must produce one warning per callback");
  if(Meshac.LOG.lines.stream().anyMatch(s->s.contains("private-token-test") || s.contains(":bad"))) throw new AssertionError("webhook URL must not leak into warning");
  checks+=2;
  var received=new java.util.concurrent.LinkedBlockingQueue<String>();
  var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/webhook",exchange->{
   received.add(exchange.getRequestMethod()+" "+exchange.getRequestHeaders().getFirst("Content-Type")+" "+new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
   exchange.sendResponseHeaders(204,-1);exchange.close();
  });
  server.start();
  try {
   Config.cur.discordWebhook="http://127.0.0.1:"+server.getAddress().getPort()+"/webhook";
   Discord.hold("test","fly","scripted");
   String hold=received.poll(3,java.util.concurrent.TimeUnit.SECONDS);
   if(hold==null || !hold.startsWith("POST application/json ") || !hold.contains("**flag** test")) throw new AssertionError("valid hold submission must remain intact");
   checks++;
   Discord.post(new Cases.Case());
   String post=received.poll(3,java.util.concurrent.TimeUnit.SECONDS);
   if(post==null || !post.contains("**kick** test") || !post.contains("case test")) throw new AssertionError("valid removal submission must remain intact");
   checks++;
  } finally {server.stop(0);}
  System.out.println("PASS discord-invalid checks="+checks);
 }
}
