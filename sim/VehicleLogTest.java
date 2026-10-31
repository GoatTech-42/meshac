package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
/** Real Vehicle, scripted boat/environment and Verdict response. No native physics. */
public class VehicleLogTest {
 static int checks;
 static void ck(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static ServerPlayer fresh(){Verdict.calls=0;return new ServerPlayer();}
 static boolean rise(ServerPlayer p){return Vehicle.check(p,new ServerboundMoveVehiclePacket(0.1));}
 public static void main(String[] args){
  for(Verdict.Step step:Verdict.Step.values()){
   var p=fresh();Verdict.next=step;
   for(int i=0;i<4;i++)ck(!rise(p),"four unsupported rises must not refuse");
   ck(rise(p)==(step!=Verdict.Step.LOG),"fifth rise refusal must follow Verdict step "+step);
   ck(Verdict.calls==1,"fifth rise must still signal once in "+step);
   ck(!rise(p),"signal must reset rising streak");Vehicle.forget(p.getUUID());
  }
  for(String exemption:new String[]{"water","ground","bubble","below-bubble","below-water","creative","spectator","not-boat","small-rise"}){
   var p=fresh();Verdict.next=Verdict.Step.HOLD;
   for(int i=0;i<4;i++)ck(!rise(p),"warmup below threshold");
   switch(exemption){
    case "water" -> p.boat.water=true;case "ground" -> p.boat.ground=true;
    case "bubble" -> p.boat.world.bubble=true;case "below-bubble" -> p.boat.world.belowBubble=true;
    case "below-water" -> p.boat.world.belowWater=true;
    case "creative" -> p.creative=true;case "spectator" -> p.spectator=true;case "not-boat" -> p.other=true;
   }
   ck(!Vehicle.check(p,new ServerboundMoveVehiclePacket(exemption.equals("small-rise")?0.049:0.1)),"exempt rise must not refuse: "+exemption);
   ck(Verdict.calls==0,"exempt rise must not signal: "+exemption);
   p.boat.water=p.boat.ground=p.boat.world.bubble=p.boat.world.belowBubble=p.boat.world.belowWater=p.creative=p.spectator=p.other=false;
   ck(!rise(p),"exemption must reset old streak: "+exemption);
   Vehicle.forget(p.getUUID());
  }
  System.out.println("PASS vehicle-policy checks="+checks);
 }
}
