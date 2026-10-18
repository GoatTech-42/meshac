#!/usr/bin/env python3
"""Offline replay for ground-spoof flags. Reads server log lines (stdin or files).
Counts "nofall ground spoof" / "ground flag in mid-air" SIGNAL lines, clusters them into episodes (gap > 10 s), and, when the log was captured
with MESHAC_TRACE + MESHAC_GROUND_TOL=compare, counts how many flags have a "[groundtol] would-have-passed" line for the same player within 2 s before.
Old logs (fp-log-v31) carry neither block nor y, so they give flag and episode counts only; the clear rate prints n/a.
Usage: ground-replay.py [--json] log... | ground-replay.py --selftest"""
import re, sys, collections
TS = re.compile(r"^\[(\d\d):(\d\d):(\d\d)\]")
FLAG = re.compile(r"SIGNAL (\S+) nofall (ground spoof|ground flag in mid-air)")
WHP = re.compile(r"\[groundtol\] would-have-passed (\S+) ")
CTX = re.compile(r"\[trace\] groundctx (\S+) flag=true feet=(\S+?)/\S+ below=(\S+?)/")
def secs(line):
    m = TS.match(line)
    return int(m.group(1)) * 3600 + int(m.group(2)) * 60 + int(m.group(3)) if m else None
def replay(lines):
    flags = collections.defaultdict(list); whp = collections.defaultdict(list); below = collections.Counter()
    for ln in lines:
        t = secs(ln)
        if t is None: continue
        m = FLAG.search(ln)
        if m: flags[m.group(1)].append(t); continue
        m = WHP.search(ln)
        if m: whp[m.group(1)].append(t); continue
        m = CTX.search(ln)
        if m: below[m.group(3)] += 1
    out = {"players": {}, "flags": 0, "episodes": 0, "cleared": 0, "compare_lines": sum(len(v) for v in whp.values()), "below": below}
    for p, ts in flags.items():
        ts.sort(); ep = 1 + sum(1 for a, b in zip(ts, ts[1:]) if b - a > 10)
        cl = sum(1 for t in ts if any(0 <= t - w <= 2 for w in whp.get(p, [])))
        out["players"][p] = (len(ts), ep, cl); out["flags"] += len(ts); out["episodes"] += ep; out["cleared"] += cl
    return out
def report(o):
    print("flags %d, episodes %d, players %d" % (o["flags"], o["episodes"], len(o["players"])))
    for p, (n, ep, cl) in sorted(o["players"].items(), key=lambda x: -x[1][0])[:10]: print("  %s flags=%d episodes=%d cleared=%d" % (p, n, ep, cl))
    if o["compare_lines"] == 0 and not o["below"]: print("clear rate: n/a (log has no groundtol/groundctx lines)")
    else: print("clear rate: %d/%d = %.0f%%" % (o["cleared"], o["flags"], 100.0 * o["cleared"] / max(1, o["flags"])))
    if o["below"]: print("block under flags:", ", ".join("%s x%d" % kv for kv in o["below"].most_common(8)))
def selftest():
    L = ["[10:00:00] [meshac] SIGNAL .a nofall ground spoof heat=1 -> log", "[10:00:03] [meshac] SIGNAL .a nofall ground spoof heat=2 -> log",
         "[10:00:30] [meshac] SIGNAL .a nofall ground flag in mid-air heat=1 -> log",
         "[09:59:59] [meshac] [groundtol] would-have-passed .a support=minecraft:chest top=64.8750 feetY=65.0000 gap=0.1250 buf=1",
         "[10:00:30] [meshac] [trace] groundctx .a flag=true feet=minecraft:air/0.0000 below=minecraft:chest/0.8750 below2=x airBelow=true ground=true prevGround=false buf=3 dy=0 dtS=0.05 exempt=false",
         "[10:00:05] [meshac] SIGNAL .b nofall ground spoof heat=1 -> log"]
    o = replay(L)
    assert o["flags"] == 4 and o["episodes"] == 3 and o["cleared"] == 1 and o["below"]["minecraft:chest"] == 1, o
    assert replay(["[10:00:00] x SIGNAL .a nofall ground spoof heat=1"])["compare_lines"] == 0
    print("ground-replay selftest ok")
if __name__ == "__main__":
    if sys.argv[1:] == ["--selftest"]: selftest()
    else:
        js = "--json" in sys.argv; args = [x for x in sys.argv[1:] if x != "--json"]
        ls = []
        for f in args or ["-"]: ls += (sys.stdin if f == "-" else open(f, errors="replace")).read().splitlines()
        o = replay(ls)
        if js:
            import json; o["below"] = dict(o["below"]); o["players"] = {p: {"flags": v[0], "episodes": v[1], "cleared": v[2]} for p, v in o["players"].items()}; print(json.dumps(o, indent=1))
        else: report(o)
