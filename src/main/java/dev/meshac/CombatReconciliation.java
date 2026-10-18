package dev.meshac;

import java.util.ArrayList;
import java.util.List;

/** Research reducer only. No widened rewind, game-state mutation, verdict or attack replay. */
public final class CombatReconciliation {
	public static final long HISTORY_NS=2_500_000_000L, LEGAL_REWIND_NS=750_000_000L;
	public static final int POSITION_CAP=256, ATTACK_CAP=64;
	public record Position(long observedNs, long receiveNs, int dimension, double x, double y, double z) {}
	public record Attack(long sequence, long observedNs, long receiveNs, int dimension, int target, long acknowledgedLowerNs, long acknowledgedUpperNs) {}
	public enum Decision { CHECK_ORIGINAL_WINDOW, AMBIGUOUS_TRANSPORT, INSUFFICIENT_HISTORY }
	public record Context(Decision decision, List<Position> positions, long lostPositions, long lostAttacks, boolean attackRetained) {}
	private final Position[] positions=new Position[POSITION_CAP];
	private final Attack[] attacks=new Attack[ATTACK_CAP];
	private long pTotal,aTotal,pLost,aLost,lastObserved=Long.MIN_VALUE;
	private int pSize,pNext,aSize,aNext;

	private void advance(long ns) {
		if(lastObserved!=Long.MIN_VALUE && ns<lastObserved){clear();}
		lastObserved=ns;
		while(pSize>0 && ns-positions[Math.floorMod(pNext-pSize,POSITION_CAP)].observedNs()>HISTORY_NS){positions[Math.floorMod(pNext-pSize,POSITION_CAP)]=null;pSize--;}
		while(aSize>0 && ns-attacks[Math.floorMod(aNext-aSize,ATTACK_CAP)].observedNs()>HISTORY_NS){attacks[Math.floorMod(aNext-aSize,ATTACK_CAP)]=null;aSize--;}
	}
	public void position(long observedNs,long receiveNs,int dimension,double x,double y,double z) {
		if(receiveNs>observedNs || !Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("position/time");
		advance(observedNs);if(pSize==POSITION_CAP)pLost++;else pSize++;
		positions[pNext]=new Position(observedNs,receiveNs,dimension,x,y,z);pNext=(pNext+1)%POSITION_CAP;pTotal++;
	}
	/** Bounds must be server-grounded acknowledgement intervals, not client timestamps or ping guesses. -1/-1 means unknown. */
	public Attack attack(long observedNs,long receiveNs,int dimension,int target,long lower,long upper) {
		if(receiveNs>observedNs || !((lower==-1&&upper==-1)||(lower>=0&&upper>=lower&&upper<=observedNs)))throw new IllegalArgumentException("attack/time bounds");
		advance(observedNs);if(aSize==ATTACK_CAP)aLost++;else aSize++;
		Attack a=new Attack(aTotal++,observedNs,receiveNs,dimension,target,lower,upper);attacks[aNext]=a;aNext=(aNext+1)%ATTACK_CAP;return a;
	}
	/** Returns context for the normal 750ms validator, never an ALLOW/HIT/REPLAY result. */
	public Context context(Attack attack,long nowNs) {
		advance(nowNs);boolean retained=false;for(int i=0;i<aSize;i++)if(attacks[Math.floorMod(aNext-aSize+i,ATTACK_CAP)].equals(attack)){retained=true;break;}
		List<Position> found=new ArrayList<>();
		for(int i=0;i<pSize;i++){Position p=positions[Math.floorMod(pNext-pSize+i,POSITION_CAP)];if(p.dimension()==attack.dimension() && p.observedNs()<=attack.observedNs())found.add(p);}
		if(!retained || found.isEmpty())return new Context(Decision.INSUFFICIENT_HISTORY,List.copyOf(found),pLost,aLost,retained);
		// Two seconds of history diagnoses delivery ordering but does NOT authorize a
		// hit from that old position. No new rewind limit is inferred from a stall.
		boolean known=attack.acknowledgedLowerNs()>=0;
		boolean inside=known && attack.acknowledgedLowerNs()>=attack.observedNs()-LEGAL_REWIND_NS;
		Decision d=inside?Decision.CHECK_ORIGINAL_WINDOW:Decision.AMBIGUOUS_TRANSPORT;
		return new Context(d,List.copyOf(found),pLost,aLost,true);
	}
	public void clear(){java.util.Arrays.fill(positions,null);java.util.Arrays.fill(attacks,null);pTotal=aTotal=pLost=aLost=0;pSize=pNext=aSize=aNext=0;lastObserved=Long.MIN_VALUE;}
	public int positionCount(){return pSize;}
	public int attackCount(){return aSize;}
}
