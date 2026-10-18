package dev.meshac;
/** An isolated packet batch is not sustained inventory automation.
 * Retains >10/100ms rate threshold; needs3overloaded windows on distinct ticks. */
public final class InventoryBurstEvidence {
 private long start=Long.MIN_VALUE,lastStrike=Long.MIN_VALUE,lastTick=Long.MIN_VALUE; private int events,strikes;
 public boolean observe(long now,long handlingTick){
  if(start==Long.MIN_VALUE || now<start || now-start>100){start=now;events=0;}
  if(++events<=10)return false;
  if(lastStrike!=Long.MIN_VALUE && (now<lastStrike || now-lastStrike>3000 || handlingTick<lastTick))strikes=0;
  if(lastTick==handlingTick || (lastStrike!=Long.MIN_VALUE && now-lastStrike<=100))return false;
  lastTick=handlingTick;lastStrike=now;
  if(++strikes<3)return false;
  strikes=0;return true;
 }
}
