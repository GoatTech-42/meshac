# Candidate9g inventory gesture and real-socket proxy follow-up

Basea99d572. Veil inputceaeb1c->c4579df. Inventory.java conflict resolved to the
worker's semantic grouping branch in full: live InventoryBurst replaces earlier
3windowInventoryBurstEvidence. This is stricter on ungrouped mixed loot/ordinary
clicks: >10/100msstill flags immediately rather than3overloaded windows. Old
pure helper/tests retained as historical comparison, not live enforcement.
Runner conflict additive union, no dropped classes. Mixin passes click-kind and
current clicked-slot item key; unknown accessor/item counts ordinary/ungrouped.

QUICK_CRAFTstage events don't count at all; same-item QUICK_MOVErun counts once
per100mswindow, BACKLOGsignal resets burst count (existingTicks.bunched3s).
Known cost: a same-item auto-steal can mimic vanilla shift-doubleclick and evade
this branch, repeated/forged backlog can defer burst evidence, unknown aliases
or state-changing slots can break grouping. No direct native trace clearance.
Item key uses identityHashCode, collision theoretical; version APIs need Box.
Known ambiguity of generic100msbeat still exists: legitimate regular sorting may
signal cadence even though burst suppression is correct. No claim all sorting FP
is solved. Preserve offhand/totem arrival consumption for every packet.

Stress60000ms:11400QUICK_CRAFTstage events->0burstsignals;11100sameitemshift
events->0burstsignals;8100mixed-itemhackevents->600burstsignals. Existing tests
pin alternatingitems, unknown items, ordinaryclicks, drag interleave positives.
Earlier raw11eventbatchstress still emits100from obsolete helper, now clearly
comparison-only; not evidence of live grouped path flagging. No invented 'all
large batches legal' exemption.

Proxy smoke restores actual numeric-loopbackTCP+UNIXcontrol+realpump+disconnect
cleanup and receipt output coverage. Lifecycle uses ready events and bounded
Eventwaits/join, no scheduler sleeps for assertions. Meter clock injected fixed;
hold release and receipt await actual forwarded500bytes. Thus real socket bytes
are tested but real250mswall deadline is not a benchmark. Deterministic clock
unit tests own exact window boundary. No real server/client/rig touched. Test
cleanup closes sockets/joins, no subprocess or kill.

58Java pure classes PASS,16rig/test*.pyentrypointsPASS including7proxytests.
The allocation-counttest still steady-state0in100000writesbatch, not JVMstartup
allocation guarantee. Full API/version compile/native legal-success/automation
controls and40playerCPU/GCremain Box/rig gates. Source-only, no deployment.
