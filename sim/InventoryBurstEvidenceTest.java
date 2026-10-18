import dev.meshac.InventoryBurstEvidence;
public final class InventoryBurstEvidenceTest {
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  InventoryBurstEvidence one=new InventoryBurstEvidence();
  for(int i=0;i<100;i++)ck(!one.observe(1000,20),"same0ms shift-click/translated batch never repeated evidence");
  InventoryBurstEvidence multi=new InventoryBurstEvidence();
  for(int w=0;w<3;w++)for(int i=0;i<11;i++){
   boolean hit=multi.observe(1000+w*200,w+20);
   ck(hit==(w==2&&i==10),"3separate overloaded windows positive, not first legit drag");
  }
  InventoryBurstEvidence stall=new InventoryBurstEvidence();
  for(int w=0;w<4;w++)for(int i=0;i<12;i++)ck(!stall.observe(1000+w*200,20),"main-thread same tick can't multiply windows");
  InventoryBurstEvidence slow=new InventoryBurstEvidence();
  for(int i=0;i<100;i++)ck(!slow.observe(i*50,i),"50ms auto control belongs to steady-gap branch not burst");
  InventoryBurstEvidence sorting=new InventoryBurstEvidence();int cleanSignals=0;
  for(int i=0;i<1200;i++)if(sorting.observe(i*50,i))cleanSignals++;
  ck(cleanSignals==0,"60second20click/s sorting model stays below burst threshold");
  InventoryBurstEvidence denseSorting=new InventoryBurstEvidence();int ambiguous=0;
  for(int w=0;w<300;w++)for(int i=0;i<11;i++)if(denseSorting.observe(1000+w*200,20+w*4))ambiguous++;
  ck(ambiguous==100,"known risk: sustained11event drag batches/200ms still emit100signals/minute");
  System.out.println("Sorting stress:1200events/60s burstSignals="+cleanSignals+";3300dense drag-stage events/60s signals="+ambiguous+" KNOWN_AMBIGUITY_NOT_NATIVE_PROOF");
  System.out.println("InventoryBurstEvidenceTest:0msbatch, same-tick stall, independent burst positive PASS");
 }
}
