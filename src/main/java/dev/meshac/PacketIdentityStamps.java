package dev.meshac;

import java.util.IdentityHashMap;

/** Decoder draft storage only, NOT injected into live Netty/Fabric. Thread-safe and bounded. */
public final class PacketIdentityStamps {
	public record Stamp(long session,long ordinal,long receivedNs) {}
	private static final int CAP=256;
	private static final long TTL=2_500_000_000L;
	private final IdentityHashMap<Object,Stamp> map=new IdentityHashMap<>();
	private long session=1,next,lost;
	public synchronized Stamp receive(Object packet,long ns){
		if(packet==null)throw new IllegalArgumentException("packet");
		map.entrySet().removeIf(e->ns<e.getValue().receivedNs() || ns-e.getValue().receivedNs()>TTL);
		Stamp old=map.get(packet);if(old!=null)return old; // main-thread redispatch cannot stamp twice
		if(map.size()>=CAP){lost++;return null;} // no unknown FIFO timestamp guessing
		Stamp stamp=new Stamp(session,next++,ns);map.put(packet,stamp);return stamp;
	}
	public synchronized Stamp consume(Object packet,long nowNs){
		Stamp s=map.remove(packet);return s!=null && s.session()==session && nowNs>=s.receivedNs() && nowNs-s.receivedNs()<=TTL?s:null;
	}
	public synchronized void disconnect(){map.clear();session++;next=0;}
	public synchronized int size(){return map.size();}
	public synchronized long lost(){return lost;}
}
