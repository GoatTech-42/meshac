import dev.meshac.CombatRecorder;
import java.util.UUID;
public final class CombatRecorderTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void hit(CombatRecorder r,UUID id,long t){r.add(id,t,1,2,.1,.2,.3,1,1.6,625,80,2.9,.1);}
 public static void main(String[] args){
  CombatRecorder r=new CombatRecorder();UUID id=UUID.randomUUID();
  hit(r,id,1);check(!r.page(id,0,1).exists(),"opt in only");
  check(r.start(id,0),"start");for(int i=0;i<600;i++)hit(r,id,i);
  var p=r.page(id,0,600);check(p.total()==600 && p.retained()==512 && p.overwritten()==88,"bounded ring and loss count");
  check(p.hits().size()==16 && p.hits().get(0).sequence()==88,"oldest-first page");
  check(r.page(id,31,600).hits().get(15).sequence()==599,"newest sample");
  check(r.page(id,-1,600).hits().isEmpty() && r.page(id,32,600).hits().isEmpty(),"bad page");
  try{p.hits().clear();throw new AssertionError("mutable snapshot");}catch(UnsupportedOperationException expected){}
  hit(r,id,120_000_000_000L);check(r.page(id,0,120_000_000_000L).total()==600,"expiry stops collection");
  check(r.page(id,0,120_000_000_000L).expired(),"expiry visible");
  for(int i=0;i<3;i++)check(r.start(UUID.randomUUID(),0),"four captures");
  check(!r.start(UUID.randomUUID(),0) && r.size()==4,"global bound");
  check(r.clear(id) && !r.active(id,1),"clear/disconnect");
  System.out.println("CombatRecorderTest: opt-in, ring, loss, paging, immutable snapshots, expiry, global bound PASS");
 }
}
