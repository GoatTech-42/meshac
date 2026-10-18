import dev.meshac.PacketCausalClock;
public final class PacketCausalClockTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args){
  var c=new PacketCausalClock();var a=c.receive(3_000_000_000L);var h=c.handled(a,3_100_000_000L,62);
  var b=c.bounds(h,true,true,true);check(b.serverQueueNs()==100_000_000L,"only server queue measured");
  check(!b.trustedLowerBound() && !b.trustedClientTick(),"nonce/client-tick/sequence not a click timestamp");
  var second=c.receive(3_000_000_001L);check(second.receiveOrder()>a.receiveOrder(),"causal arrival order");
  // Both a delayed old click and newly generated click have identical receive stamps.
  check(c.bounds(h,false,false,false).latestPossibleGenerationNs()==3_000_000_000L,"receive is upper bound only");
  c.newSession();check(!c.bounds(h,true,true,true).valid(),"no cross-session mapping");
  try{c.handled(a,4_000_000_000L,80);throw new AssertionError("old session");}catch(IllegalArgumentException expected){}
  System.out.println("PacketCausalClockTest: no false trusted click-time mapping, order, queue and lifecycle PASS");
 }
}
