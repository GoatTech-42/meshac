#!/usr/bin/env python3
"""Combat wiring pins across EVERY source set (0048). Written when the 0043 no-caller claim
was retracted: that claim came from a src/main-scoped grep that missed the per-version attack
source sets. These pins read ALL of them, strip comments before asserting code tokens (the
0046 lesson: a pin that can pass on comment text is vacuous), and carry an in-process,
non-recursive mutation control. SOURCE-SHAPE ONLY: they pin wiring text - they do not prove
compilation (Box gates that) or runtime behaviour."""
import io, contextlib, pathlib, re, sys

ROOT = pathlib.Path(__file__).parent.parent
def read(p): return (ROOT / p).read_text()
def nocomments(s):
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    return re.sub(r'//[^\n]*', '', s)

A26, A1211 = 'src/attack26/java/dev/meshac/mixin/AttackMixin.java', 'src/attack1211/java/dev/meshac/mixin/AttackMixin.java'
MAINMIX, COMBAT = 'src/main/java/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java', 'src/main/java/dev/meshac/Combat.java'

def versions_map():
    out = {}
    for p in (ROOT / 'versions').glob('*.properties'):
        for line in p.read_text().splitlines():
            if line.startswith('attack_src='): out[p.stem] = line.split('=', 1)[1].strip()
    return out

def run(a26, a1211, mainmix, combat, mixins_json, gradle, versions, meshac):
    lines, f = [], 0
    def c(ok, what):
        nonlocal f
        if not ok: lines.append('FAIL ' + what); f += 1
    c('@Inject(method = "handleAttack"' in a26 and 'ServerboundAttackPacket' in a26,
      'attack26 injects handleAttack(ServerboundAttackPacket)')
    c('Combat.check(player, e)' in a26 and 'Verdict.signal(player, "combat", hit, 2)' in a26,
      'attack26 calls Combat.check -> Verdict.signal weight 2 (the live d124 combat path)')
    c('if (e == null) return' in a26 and 'isSameThread()' in a26, 'attack26 keeps the null-target + same-thread gates')
    c('handleInteract' in a1211 and 'onAttack()' in a1211 and 'p.getTarget(player.level())' in a1211,
      'attack1211 routes interact onAttack -> target')
    c('Combat.check(player, e)' in a1211 and 'Verdict.signal(player, "combat", hit, 2)' in a1211,
      'attack1211 calls Combat.check -> Verdict.signal weight 2')
    c('"AttackMixin"' in mixins_json, 'meshac.mixins.json registers AttackMixin')
    c('attack_src' in gradle and 'src/${project.attack_src}/java' in gradle, 'build.gradle wires the per-version attack source set')
    c(versions.get('26.1') == 'attack26' and versions.get('26.2') == 'attack26' and versions.get('26.3') == 'attack26'
      and versions.get('1.21.11') == 'attack1211',
      'versions/*.properties map attack_src (Box javap 10-17: split-packet API identical on 26.1.2/26.2/26.3)')
    # revert guards: the withdrawn 0044/0046 emit drafts must stay out
    c('handleAttack' not in mainmix and 'ServerboundAttackPacket' not in mainmix,
      'main packet mixin has NO attack hook (0044/0046 withdrawn; the 0046 one also broke the 1.21.11 build)')
    c('evaluateAndLog' not in combat and 'public static String evaluate(' not in combat,
      'Combat.java carries no evaluate/evaluateAndLog (0046 withdrawn)')
    # the wiring the 0043 analysis got right
    c('Combat.tick(pl, server.getTickCount())' in meshac, 'Combat.tick stays wired (END_SERVER_TICK)')
    c('Combat.swapped(player, p.getSlot())' in mainmix, 'Combat.swapped stays wired (hotbar swap)')
    return f, lines

failed = 0
def check(ok, what):
    global failed
    if not ok: print('FAIL ' + what); failed += 1

args = [nocomments(read(A26)), nocomments(read(A1211)), nocomments(read(MAINMIX)), nocomments(read(COMBAT)),
        read('src/main/resources/meshac.mixins.json'), read('build.gradle'), versions_map(), nocomments(read('src/main/java/dev/meshac/Meshac.java'))]
f, lines = run(*args)
for l in lines: print(l)
check(f == 0, 'real tree passes every combat wiring pin (%d failures)' % f)

# mutation control (in-process, non-recursive): strip the Combat.check call from attack26 and
# the run MUST fail with exactly the one wiring FAIL line - not a crash, not a pass.
mut_args = [args[0].replace('Combat.check(player, e)', '')] + args[1:]
buf = io.StringIO()
with contextlib.redirect_stdout(buf):
    mf, mlines = run(*mut_args)
check(mf == 1, 'mutation control: removing Combat.check from attack26 fails exactly one pin (got %d)' % mf)
check(mlines == ['FAIL attack26 calls Combat.check -> Verdict.signal weight 2 (the live d124 combat path)'],
      'mutation control emits the exact expected FAIL line: ' + str(mlines))
if failed: raise SystemExit('%d failures' % failed)
print('test-combat-wiring ok')
