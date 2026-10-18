package dev.meshac;

/** Causal arrival stamps only. Neither an ACK nor TCP order authenticates a click time. */
public final class PacketCausalClock {
	public record Stamp(long session, long receiveOrder, long receivedNs, long handledNs, int handledTick) {}
	public record Bounds(long latestPossibleGenerationNs, long serverQueueNs, boolean trustedLowerBound, boolean trustedClientTick, boolean valid) {}
	private long session=1,nextOrder;
	/** Call once at the actual decoder/connection event loop, never again on main-thread redispatch. */
	public Stamp receive(long ns){return new Stamp(session,nextOrder++,ns,-1,-1);}
	public Stamp handled(Stamp stamp,long ns,int tick){
		if(stamp.session()!=session || ns<stamp.receivedNs())throw new IllegalArgumentException("session/time");
		return new Stamp(session,stamp.receiveOrder(),stamp.receivedNs(),ns,tick);
	}
	/** Echoed server nonce proves server-message receipt, not click generation. */
	public Bounds bounds(Stamp stamp,boolean echoedNonce,boolean clientTickEnd,boolean useItemSequence){
		boolean valid=stamp.session()==session && stamp.handledNs()>=stamp.receivedNs();
		return new Bounds(stamp.receivedNs(),valid?stamp.handledNs()-stamp.receivedNs():-1,false,false,valid);
	}
	public void newSession(){session++;nextOrder=0;}
}
