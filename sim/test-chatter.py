#!/usr/bin/env python3
"""Pinned real Chatter with deterministic sequential clock/player doubles.
No command execution, callback concurrency or network-arrival qualification.
"""
import argparse,hashlib,os,resource,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=root/'src/main/java/dev/meshac/Chatter.java'
expected='f5c9f22a0380673f903de6c7755f3d05d2e5b85d4c1c4f2984f76899bbacee21'
actual=hashlib.sha256(source.read_bytes()).hexdigest();assert actual==expected,('Chatter drift',actual)
text=source.read_text();assert text.count('System.currentTimeMillis()')==1
copy=text.replace('System.currentTimeMillis()','TestClock.ms');assert copy.replace('TestClock.ms','System.currentTimeMillis()')==text
mutants={
 'early':('s.q.size() < MIN_COMMANDS','s.q.size() < MIN_COMMANDS - 1'),
 'spread':('max - min <= MAX_SPREAD_MS','max - min < MAX_SPREAD_MS'),
 'burst':('max >= 150','max >= 149'),
 'window':('> WINDOW_MS','>= WINDOW_MS'),
 'distinct':('args.size() == s.q.size()','true'),
 'signal-reset':('s.q.clear();\n\t\t\treturn', '/* retained */\n\t\t\treturn'),
 'root':('s.q.clear(); s.root = root;','s.root = root;'),
 'forget':('STATE.remove(id);',''),
 'clear-all':('STATE.remove(id);','STATE.clear();'),
}
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--mutant',choices=mutants);args=p.parse_args()
if args.mutant:
 old,new=mutants[args.mutant];assert copy.count(old)==1;copy=copy.replace(old,new)
print('SOURCE',source,actual,'mutant',args.mutant,flush=True)
sources={
 'dev/meshac/Chatter.java':copy,
 'dev/meshac/ChatterTest.java':(root/'sim/ChatterTest.java').read_text(),
 'dev/meshac/TestClock.java':'package dev.meshac;class TestClock {static long ms=100000;}',
 'net/minecraft/server/level/ServerPlayer.java':'package net.minecraft.server.level;public class ServerPlayer {final java.util.UUID id=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return id;}}',
}
with tempfile.TemporaryDirectory(prefix='chatter-state-') as directory:
    directory=Path(directory)
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
    run(['java', '-Xmx256m', '-cp', str(directory / 'out'), 'dev.meshac.ChatterTest'])
