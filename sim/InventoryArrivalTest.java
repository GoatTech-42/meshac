package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
/** Real Inventory arrival pairing, deterministic clocks and external policy double. */
public class InventoryArrivalTest {
 static int checks;
 static void ck(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 public static void main(String[] args){
  for(String exemption:new String[]{"creative","spectator","config"}){
   var p=new ServerPlayer();Verdict.calls=0;Config.exempt=exemption.equals("config");p.creative=exemption.equals("creative");p.spectator=exemption.equals("spectator");
   boolean resumeOnly=args.length>0 && args[0].equals("resume");
   for(int i=0;i<100;i++){
    TestClock.ms=100000+i*1000;Inventory.arrive(p);TestClock.ms+=37;
    long result=Inventory.click(p);
    if(!resumeOnly)ck(result==100000+i*1000,"exempt click must consume and return own network arrival: "+exemption);
   }
   ck(Verdict.calls==0,"exempt click stream must not signal");
   Config.exempt=p.creative=p.spectator=false;
   TestClock.ms=300000;Inventory.arrive(p);TestClock.ms+=19;
   ck(Inventory.click(p)==300000,"exemption end must not replay stale queued arrivals");
   TestClock.ms=301000;ck(Inventory.click(p)==301000,"unpaired click must fall back to current clock");
   Inventory.forget(p.getUUID());
  }
  var normal=new ServerPlayer();Verdict.calls=0;Config.exempt=false;
  for(int i=0;i<12;i++){
   TestClock.ms=500000+i*100;Inventory.arrive(normal);TestClock.ms+=40;
   ck(Inventory.click(normal)==500000+i*100,"normal click must retain network cadence");
  }
  ck(Verdict.calls>0,"real steady 100ms cadence must still signal");
  Inventory.forget(normal.getUUID());
  System.out.println("PASS inventory-arrival checks="+checks);
 }
}
