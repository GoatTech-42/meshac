import dev.meshac.CombatRecorder;
import java.util.UUID;
public final class Recorder40Benchmark {
 public static void main(String[] args){
  UUID[] ids=new UUID[40];for(int i=0;i<40;i++)ids[i]=new UUID(0,i+1);
  CombatRecorder r=new CombatRecorder();int enabled=0;for(UUID id:ids)if(r.start(id,0))enabled++;
  long start=System.nanoTime(),calls=0;
  for(int tick=0;tick<1200;tick++)for(UUID id:ids){r.add(id,tick*50_000_000L,tick,2,.1,.2,.3,1,1.6,50,80,3,.1);calls++;}
  long elapsed=System.nanoTime()-start,total=0,retained=0;for(UUID id:ids){var p=r.page(id,0,60_000_000_000L);total+=p.total();retained+=p.retained();}
  System.out.printf("players=40 requested=40 enabled=%d calls=%d collected=%d retained=%d elapsedMs=%.4f nsPerAttempt=%.2f automaticLogLines=0%n",enabled,calls,total,retained,elapsed/1e6,(double)elapsed/calls);
 }
}
