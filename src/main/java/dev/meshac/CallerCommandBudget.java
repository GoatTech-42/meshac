package dev.meshac;

import java.util.HashMap;
import java.util.Map;

/** At most four accepted dump commands/caller/minute; unknown overflow callers fail closed. */
public final class CallerCommandBudget {
	private static final long WINDOW=60_000_000_000L;
	private static final int MAX_CALLERS=64, LIMIT=4;
	private static final class Window {long start;int used;Window(long ns){start=ns;}}
	private final Map<String,Window> callers=new HashMap<>();
	public boolean allow(String key,long ns){
		callers.entrySet().removeIf(e->ns<e.getValue().start || ns-e.getValue().start>=WINDOW);
		Window w=callers.get(key);
		if(w==null){if(callers.size()>=MAX_CALLERS)return false;w=new Window(ns);callers.put(key,w);}
		if(w.used>=LIMIT)return false;w.used++;return true;
	}
	public int size(){return callers.size();}
}
