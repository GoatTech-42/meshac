#!/usr/bin/env python3
"""TEXT2CHECK: bind a [meshac] SIGNAL log line to its check.

The check name is a STRUCTURED field in every SIGNAL line (Verdict.java:
"[meshac] SIGNAL <player> <check> <detail> heat=N -> <step>"), so binding is an exact parse,
not fuzzy detail text. The old grader matched five detail words ('air jump', 'step', 'nofall',
'ground spoof', 'blink'); every other check's SIGNAL (fly, speed, combat, ...) graded as silence
even when the jar flagged - an expect=signal fixture for any of those checks could never PASS.

CHECKS is pinned to CheckNames.ALL in the jar source; test-signal-checks.py scans CheckNames.java
and fails on drift. TEXT2CHECK records one representative detail-text fragment per check for
fixture authors and diagnostics only - binding itself never looks at detail text."""
import re

SIGNAL = re.compile(r"SIGNAL \S+ (\S+)(?:\s(.*))?$") # prefixes before SIGNAL allowed (timestamps, logger tags)
CHECKS = frozenset((
    "fly", "speed", "step", "jump", "air", "high", "ladder", "levitation", "microhop", "timer",
    "nofall", "noclip", "blink", "movement", "antiknockback", "combat", "interact", "inventory",
    "mining", "mining-lookray", "glide", "boatfly", "chat",
))
TEXT2CHECK = {
    "fly": "hover/ascend", "speed": "speed", "step": "step dy / rose", "jump": "jump dy",
    "air": "air jump dy", "high": "high jump rose", "ladder": "ladder", "levitation": "levitation",
    "microhop": "microhop", "timer": "timer", "nofall": "ground spoof / ground flag in mid-air",
    "noclip": "moving inside solid blocks", "blink": "held its movement packets back",
    "movement": "slippery ground", "antiknockback": "no push after three hits",
    "combat": "reach/aim", "interact": "clicked/placed/uses", "inventory": "item moves / hotbar slot",
    "mining": "block broken / work rate", "mining-lookray": "starts digging 40+ degrees away",
    "glide": "restarted gliding / no air drag", "boatfly": "boat climbing in open air", "chat": "chat",
}

def bind(line):
    """The check a SIGNAL line is for, or None when the line is not a SIGNAL for a known check."""
    m = SIGNAL.search(line)
    return m.group(1) if m and m.group(1) in CHECKS else None

def bound(lines):
    """The set of checks bound across the given log lines (empty when nothing flagged)."""
    out = set()
    for l in lines:
        c = bind(l)
        if c: out.add(c)
    return out
