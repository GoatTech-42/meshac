#!/usr/bin/env python3
"""Run real Ticks clock-boundary and lifecycle checks with a minimal UUID double.
No packet hook, thread or native-client proof. Optional --mutant NAME demonstrates
that an authored assertion catches that particular production-code mutation.
"""
import argparse
import hashlib
import os
from pathlib import Path
import resource
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
source = root / 'src/main/java/dev/meshac/Ticks.java'
expected = '4c8fb3bad26a72c79d1d4d1637cdcd1e849c505667ca9ee430a24ca671774026'
actual = hashlib.sha256(source.read_bytes()).hexdigest()
assert actual == expected, ('Ticks source drift: review and repin before running', actual, expected)
text = source.read_text()
assert text.count('System.currentTimeMillis()') == 3
copy = text.replace('System.currentTimeMillis()', 'TestClock.ms')
assert copy.replace('TestClock.ms', 'System.currentTimeMillis()') == text
mutants = {
    'activation': ('s[0] > 40', 's[0] >= 40'),
    'stale': ('- s[1] < 500', '- s[1] <= 500'),
    'early-backlog': ('s[3] >= 3', 's[3] >= 2'),
    'expiry': ('TestClock.ms < s[4]', 'TestClock.ms <= s[4]'),
    'gap': ('- s[1] < 5 ?', '- s[1] <= 5 ?'),
    'forget': ('S.remove(id);', ''),
    'clear-all': ('S.remove(id);', 'S.clear();'),
}
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--mutant', choices=mutants)
args = parser.parse_args()
if args.mutant:
    old, new = mutants[args.mutant]
    assert copy.count(old) == 1
    copy = copy.replace(old, new)
print('SOURCE', source, actual, 'mutant', args.mutant, flush=True)
with tempfile.TemporaryDirectory(prefix='tick-counter-') as directory:
    directory = Path(directory)
    sources = {
        'dev/meshac/Ticks.java': copy,
        'dev/meshac/TicksTest.java': (root / 'sim/TicksTest.java').read_text(),
        'dev/meshac/TestClock.java': 'package dev.meshac; class TestClock { static long ms = 100000; }',
        'net/minecraft/server/level/ServerPlayer.java': '''package net.minecraft.server.level;
public class ServerPlayer {
 private final java.util.UUID id = java.util.UUID.randomUUID();
 public java.util.UUID getUUID() { return id; }
}''',
    }
    files = []
    for relative, body in sources.items():
        path = directory / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body)
        files.append(str(path))
    env = {k: v for k, v in os.environ.items() if not k.startswith('MESHAC_')}
    def run(command):
        with tempfile.TemporaryFile() as log:
            result = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT,
                env=env, timeout=30,
                preexec_fn=lambda: resource.setrlimit(resource.RLIMIT_FSIZE, (4194304, 4194304)))
            assert log.tell() < 4194304, 'output cap'
            log.seek(0)
            print(log.read().decode(), end='', flush=True)
            assert result.returncode == 0, (command[0], result.returncode)
    run(['javac', '-J-Xmx256m', '-d', str(directory / 'out')] + files)
    run(['java', '-Xmx256m', '-cp', str(directory / 'out'), 'dev.meshac.TicksTest'])
