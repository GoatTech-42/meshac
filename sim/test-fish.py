#!/usr/bin/env python3
"""Real Fish state machine with deterministic clock and focused runtime doubles.
Not native fishing, hook/accessor application, latency measurement or Verdict proof.
"""
import argparse,hashlib,os,resource,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=root/'src/main/java/dev/meshac/Fish.java'
expected='84b1840e477d046d423831bf934e7f65b75424fa43fb3d080b20d38ecedf19dc'
actual=hashlib.sha256(source.read_bytes()).hexdigest()
assert actual==expected,('Fish source drift: review and repin',actual)
text=source.read_text()
assert text.count('System.currentTimeMillis()')==2
copy=text.replace('System.currentTimeMillis()','TestClock.ms')
assert copy.replace('TestClock.ms','System.currentTimeMillis()')==text
mutants={
 'threshold':('reaction < FAST_MS','reaction <= FAST_MS'),
 'early':('++s.fast >= STREAK','++s.fast >= STREAK - 1'),
 'no-reset':('else s.fast = 0;','else {}'),
 'repeat':('s.biting = false;\n\t\tif (reaction','/*keep armed*/\n\t\tif (reaction'),
 'upper-latency':('Math.min(300, pl.connection.latency())','pl.connection.latency()'),
 'lower-latency':('Math.max(0, Math.min(300, pl.connection.latency()))','Math.min(300, pl.connection.latency())'),
 'refresh':('nib && !s.biting','nib'),
 'forget':('STATE.remove(id);',''),
 'clear-all':('STATE.remove(id);','STATE.clear();'),
}
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--mutant',choices=mutants);args=p.parse_args()
if args.mutant:
 old,new=mutants[args.mutant];assert copy.count(old)==1;copy=copy.replace(old,new)
print('SOURCE',source,actual,'mutant',args.mutant,flush=True)
sources={
 'dev/meshac/Fish.java':copy,
 'dev/meshac/FishTest.java':(root/'sim/FishTest.java').read_text(),
 'dev/meshac/TestClock.java':'package dev.meshac; class TestClock {static long ms=100000;}',
 'dev/meshac/Verdict.java':'''package dev.meshac;class Verdict {static int calls;static String detail;static void signal(net.minecraft.server.level.ServerPlayer p,String check,String text,int weight){if(!check.equals("interact")||weight!=1)throw new AssertionError("signal identity");calls++;detail=text;}}''',
 'dev/meshac/mixin/FishingHookAccess.java':'package dev.meshac.mixin;public interface FishingHookAccess {int meshac$nibble();}',
 'net/minecraft/world/entity/projectile/FishingHook.java':'package net.minecraft.world.entity.projectile;public class FishingHook implements dev.meshac.mixin.FishingHookAccess {public int nibble;public int meshac$nibble(){return nibble;}}',
 'net/minecraft/server/level/ServerPlayer.java':'''package net.minecraft.server.level;public class ServerPlayer {final java.util.UUID id=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return id;}public boolean creative;public boolean isCreative(){return creative;}public net.minecraft.world.entity.projectile.FishingHook fishing;public final Connection connection=new Connection();public static class Connection {public int ms;public int latency(){return ms;}}}''',
 'net/minecraft/server/MinecraftServer.java':'''package net.minecraft.server;public class MinecraftServer {public final java.util.List<net.minecraft.server.level.ServerPlayer> players=new java.util.ArrayList<>();public MinecraftServer getPlayerList(){return this;}public java.util.List<net.minecraft.server.level.ServerPlayer> getPlayers(){return players;}}''',
}
with tempfile.TemporaryDirectory(prefix='fish-state-') as directory:
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
    run(['java', '-Xmx256m', '-cp', str(directory / 'out'), 'dev.meshac.FishTest'])
