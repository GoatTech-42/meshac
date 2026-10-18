import dev.meshac.PacketIdentityStamps;
public final class PacketIdentityStampsTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static final class EqualPacket {public boolean equals(Object o){return true;}public int hashCode(){return 1;}}
 public static void main(String[]args){
  var r=new PacketIdentityStamps();Object a=new EqualPacket(),b=new EqualPacket();var x=r.receive(a,0);var y=r.receive(b,1);
  check(x.ordinal()!=y.ordinal(),"identity not equals");check(r.receive(a,2).equals(x),"redispatch retains original receive time");
  check(r.consume(b,3).equals(y)&&r.consume(a,3).equals(x),"out-of-order handling no FIFO misassociation");check(r.consume(a,4)==null,"one consumption");
  for(int i=0;i<256;i++)r.receive(new Object(),10);check(r.receive(new Object(),11)==null&&r.lost()==1,"overflow explicit");
  r.receive(new Object(),3_000_000_000L);check(r.size()==1,"TTL clears abandoned packet references");
  var p=new Object();r.receive(p,3_000_000_001L);r.disconnect();check(r.consume(p,3_000_000_002L)==null&&r.size()==0,"disconnect session clear");
  System.out.println("PacketIdentityStampsTest: identity, redispatch, reorder, consume, cap, TTL and session PASS");
 }
}
