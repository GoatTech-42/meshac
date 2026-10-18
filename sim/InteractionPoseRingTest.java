import dev.meshac.InteractionPoseRing;
import java.lang.management.ManagementFactory;
public final class InteractionPoseRingTest {
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  InteractionPoseRing ring=new InteractionPoseRing(6);Object dim="world";
  ck(!ring.valid(0,1000,dim),"unwritten");
  for(int i=0;i<7;i++)ring.add(1000+i,dim,i,1.62,0,0,0);
  ck(ring.x(0)==6 && ring.x(1)==1,"wraps exactly6slots");
  ck(ring.endZ(0,8)==8 && ring.endX(0,8)==6,"coherent eye/ray");
  ck(!ring.valid(0,999,dim)&&!ring.valid(0,1007,"nether")&&!ring.valid(0,1757,dim),"age/dimension");
  var bean=ManagementFactory.getThreadMXBean();
  ck(bean instanceof com.sun.management.ThreadMXBean,"allocation instrumentation unavailable");
  var alloc=(com.sun.management.ThreadMXBean)bean;
  ck(alloc.isThreadAllocatedMemorySupported(),"allocation counter unsupported");
  alloc.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().threadId();
  for(int i=0;i<100000;i++)ring.add(i,dim,1,2,3,i%360,0);
  for(int i=0;i<1000;i++)alloc.getThreadAllocatedBytes(id);
  long bytes=Long.MAX_VALUE;
  for(int batch=0;batch<5;batch++){
   long before=alloc.getThreadAllocatedBytes(id);
   for(int i=0;i<100000;i++)ring.add(i,dim,1,2,3,i%360,0);
   bytes=Math.min(bytes,alloc.getThreadAllocatedBytes(id)-before);
  }
  ck(bytes==0,"100000rotation writes allocated "+bytes+"bytes");
  System.out.println("InteractionPoseRingTest:6slot coherent wrap, age/dimension;100000writes allocated0bytes PASS");
 }
}
