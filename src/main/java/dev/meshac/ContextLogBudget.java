package dev.meshac;
/** Monotonic elapsed-time budget for optional diagnostic lines; server-thread owned. */
public final class ContextLogBudget {
 public static final int GLOBAL_LINES=20;
 public static final long GLOBAL_NS=1_000_000_000L,CALLER_NS=5_000_000_000L;
 public static final class Caller {private boolean emitted;private long at,dropped;public long dropped(){return dropped;}}
 private boolean started;private long start,dropped;private int used;
 private static long inc(long n){return n==Long.MAX_VALUE?n:n+1;}
 public boolean take(Caller c,long now){
  if(!started||now<start||now-start>=GLOBAL_NS){started=true;start=now;used=0;}
  if(c.emitted&&now<c.at)c.emitted=false;
  if((c.emitted&&now-c.at<CALLER_NS)||used>=GLOBAL_LINES){c.dropped=inc(c.dropped);dropped=inc(dropped);return false;}
  used++;c.emitted=true;c.at=now;return true;
 }
 public long dropped(){return dropped;}
}
