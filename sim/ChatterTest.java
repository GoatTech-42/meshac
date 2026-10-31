package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
/** Sequential scripted arrival times; no network threading or actual command execution. */
public class ChatterTest {
 static int checks;static void ck(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static String cmd(ServerPlayer p,String root,String arg,long gap){TestClock.ms+=gap;return Chatter.command(p,root+" "+arg);}
 static void quiet(ServerPlayer p,String root,String arg,long gap,String m){ck(cmd(p,root,arg,gap)==null,m);}
 static String cadence(ServerPlayer p,String root,long gap){for(int i=0;i<7;i++)quiet(p,root,"target"+i,gap,"first seven quiet #"+i);return cmd(p,root,"target7",gap);}
 public static void main(String[] args){
  var forgotten=new ServerPlayer();for(int i=0;i<7;i++)quiet(forgotten,"tpa","t"+i,1000,"forget cold prefix"+i);Chatter.forget(forgotten.getUUID());quiet(forgotten,"tpa","last",1000,"forget clears seven-command queue");
  var a=new ServerPlayer();String result=cadence(a,"/tpa",150);ck(result!=null&&result.contains("/tpa 8 times")&&result.contains("150 ms"),"eight distinct150ms commands signal");
  quiet(a,"tpa","again",150,"signal clears queue");Chatter.forget(a.getUUID());
  ck(cadence(a,"tpa",149)==null,"149ms burst stays quiet");Chatter.forget(a.getUUID());
  for(int i=0;i<7;i++)quiet(a,"tpa","t"+i,150,"spread60 prefix"+i);ck(cmd(a,"tpa","last",210)!=null,"exact60ms spread allowed");Chatter.forget(a.getUUID());
  for(int i=0;i<7;i++)quiet(a,"tpa","t"+i,150,"spread61 prefix"+i);quiet(a,"tpa","last",211,"61ms spread stays quiet");Chatter.forget(a.getUUID());
  for(int i=0;i<8;i++)quiet(a,"tpa","same",1000,"same argument never signals #"+i);Chatter.forget(a.getUUID());
  for(int i=0;i<7;i++)quiet(a,"tpa","t"+i,1000,"seven before root switch"+i);quiet(a,"home","other",1000,"root switch clears prior root");Chatter.forget(a.getUUID());
  for(int i=0;i<7;i++)quiet(a,i%2==0?"/TPA":"tpa","t"+i,1000,"root normalization prefix"+i);ck(cmd(a,"/Tpa","last",1000)!=null,"slash/case normalized root");Chatter.forget(a.getUUID());
  quiet(a,"tpa","first",0,"window start");for(int i=1;i<7;i++)quiet(a,"tpa","t"+i,2857,"window prefix"+i);ck(cmd(a,"tpa","last",2858)!=null,"oldest exactly20s retained");Chatter.forget(a.getUUID());
  quiet(a,"tpa","first",0,"expired window start");for(int i=1;i<7;i++)quiet(a,"tpa","t"+i,2857,"expired window prefix"+i);quiet(a,"tpa","last",2859,"oldest20001ms evicted");Chatter.forget(a.getUUID());
  var b=new ServerPlayer();for(int i=0;i<7;i++)quiet(a,"tpa","t"+i,1000,"UUIDA prefix"+i);quiet(b,"tpa","b",1000,"UUIDB isolated");Chatter.forget(b.getUUID());ck(cmd(a,"tpa","last",1000)==null,"2s gap correctly breaks steady cadence");
  Chatter.forget(a.getUUID());for(int i=0;i<7;i++)quiet(a,"tpa","t"+i,1000,"forget other prefix"+i);Chatter.forget(b.getUUID());ck(cmd(a,"tpa","last",1000)!=null,"forget other UUID preserves queue");
  for(int i=0;i<7;i++)quiet(b,"tpa","t"+i,1000,"forget own prefix"+i);Chatter.forget(b.getUUID());quiet(b,"tpa","last",1000,"forget own UUID clears queue");
  Chatter.forget(a.getUUID());for(int i=0;i<7;i++)quiet(a,"tpa","   t"+i+"   ",1000,"trimmed argument prefix"+i);quiet(a,"tpa","t6",1000,"trimmed duplicate blocks distinctness");
  Chatter.forget(a.getUUID());for(int i=0;i<8;i++){TestClock.ms+=1000;ck(Chatter.command(a,"help")==null,"no-argument command stays quiet #"+i);}
  System.out.println("PASS chatter checks="+checks);
 }
}
