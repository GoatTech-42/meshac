package dev.meshac;
/** Server-thread-owned compare diagnostics budget. Time is server ticks, not wall time.
 * Global4lines/20ticks; caller1line/100ticks. Caller state lives in Movement.S,
 * so no global UUID registry. Suppressed crossings survive motion balance resets. */
public final class CompareLogBudget {
 public static final int GLOBAL_LINES=4, GLOBAL_TICKS=20, CALLER_TICKS=100;
 public static final class Caller {
  private long last=Long.MIN_VALUE, suppressed;
  public long suppressed() {return suppressed;}
 }
 private long start=Long.MIN_VALUE; private int used; private long suppressed;
 private static long add(long n){return n==Long.MAX_VALUE?n:n+1;}
 public boolean take(Caller caller,long tick) {
  if(start==Long.MIN_VALUE || tick<start || tick-start>=GLOBAL_TICKS){start=tick;used=0;}
  if(caller.last!=Long.MIN_VALUE && tick<caller.last)caller.last=Long.MIN_VALUE;
  if((caller.last!=Long.MIN_VALUE && tick-caller.last<CALLER_TICKS)||used>=GLOBAL_LINES){
   caller.suppressed=add(caller.suppressed);suppressed=add(suppressed);return false;
  }
  used++;caller.last=tick;return true;
 }
 public long suppressed(){return suppressed;}
}
