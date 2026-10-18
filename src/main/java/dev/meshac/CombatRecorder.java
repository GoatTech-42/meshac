package dev.meshac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Explicit short staff captures only. Bounded RAM, no disk, logging or verdicts. */
public final class CombatRecorder {
	public static final int CAPACITY=512, MAX_PLAYERS=4, PAGE_SIZE=16;
	public static final long LIFETIME_NS=120_000_000_000L;
	public record Hit(long sequence, long ns, int tick, int target, double lateral, double vertical, double targetSideSpeed, double charge, double attackSpeed, long gapHandlingMs, int pingMs, double reach, double aimError) {}
	public record Page(boolean exists, boolean expired, long total, long overwritten, int retained, int pages, List<Hit> hits) {}
	private static final class Capture {
		final long start;
		final Hit[] ring=new Hit[CAPACITY];
		long total;
		Capture(long now){start=now;}
	}
	private final Map<UUID,Capture> captures=new HashMap<>();
	private final java.util.function.BooleanSupplier serverThread;
	public CombatRecorder(){Thread owner=Thread.currentThread();this.serverThread=()->Thread.currentThread()==owner;}
	public CombatRecorder(java.util.function.BooleanSupplier serverThread){this.serverThread=java.util.Objects.requireNonNull(serverThread);}
	private void guard(){if(!serverThread.getAsBoolean())throw new IllegalStateException("Combat recorder requires the server thread");}
	private static boolean expired(Capture c,long now){return now<c.start || now-c.start>=LIFETIME_NS;}
	/** Expired data is replaced only by explicit start; other active captures stay intact. */
	public boolean start(UUID id,long now){guard();
		if(!captures.containsKey(id) && captures.size()>=MAX_PLAYERS)return false;
		captures.put(id,new Capture(now));return true;
	}
	public boolean active(UUID id,long now){guard();Capture c=captures.get(id);return c!=null && !expired(c,now);}
	public void add(UUID id,long now,int tick,int target,double lat,double vert,double speed,double charge,double attackSpeed,long gap,int ping,double reach,double err){guard();
		Capture c=captures.get(id);if(c==null || expired(c,now))return;
		long seq=c.total++;c.ring[(int)(seq%CAPACITY)]=new Hit(seq,now,tick,target,lat,vert,speed,charge,attackSpeed,gap,ping,reach,err);
	}
	public Page page(UUID id,int page,long now){guard();
		Capture c=captures.get(id);if(c==null)return new Page(false,false,0,0,0,0,List.of());
		int retained=(int)Math.min(CAPACITY,c.total),pages=(retained+PAGE_SIZE-1)/PAGE_SIZE;
		List<Hit> result=new ArrayList<>();
		if(page>=0 && page<pages){long first=c.total-retained+(long)page*PAGE_SIZE;int count=Math.min(PAGE_SIZE,retained-page*PAGE_SIZE);for(int i=0;i<count;i++)result.add(c.ring[(int)((first+i)%CAPACITY)]);}
		return new Page(true,expired(c,now),c.total,Math.max(0,c.total-CAPACITY),retained,pages,List.copyOf(result));
	}
	public boolean clear(UUID id){guard();return captures.remove(id)!=null;}
	public int size(){guard();return captures.size();}
}
