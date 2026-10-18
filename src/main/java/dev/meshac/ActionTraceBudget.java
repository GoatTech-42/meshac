package dev.meshac;
/** Global bounded trace output. No per-player unbounded registry. */
public final class ActionTraceBudget {
 private long start = Long.MIN_VALUE; private int used; private long dropped;
 public synchronized boolean take(long now) {
  if (start == Long.MIN_VALUE || now < start || now - start >= 1000) {start=now;used=0;}
  if (used >= 200) {dropped++;return false;} used++;return true;
 }
 public synchronized long dropped() { return dropped; }
 public static String text(String s) { return s.replace('\n',' ').replace('\r',' ').substring(0, Math.min(s.length(), 512)); }
}
