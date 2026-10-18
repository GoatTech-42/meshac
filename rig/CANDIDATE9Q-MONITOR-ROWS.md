# Candidate9q monitor invariant: 37-row coverage

Source adapter run, not live Minecraft execution. `executed-real` below means actual production class/method or callback body compiled with instrumented service doubles. Config.monitor/enforce, selected Interact/Glide and packet callbacks are extracted verbatim from the checked tree. Only wall-clock calls are rebound to a deterministic fixture clock. Combat.check is a forced nonnull double for attack callback tests, not a real reach detector fixture. Other geometry/API collaborators are doubles. This proves the selected action decisions, not runtime mixin application or real gameplay.

9p comparison uses immutable b253252 via MESHAC_TEST_TREE; baseline mode runs isolated scenarios and emits ROW_RESULT. No absent adapter is treated as a pass. Runtime rows without a 9p measurement are explicitly unmeasured, not inferred red/green. `source-structure-only` means a guard/assertion or pure helper passes, while required real trigger remains `rig-required`.

Commands: `python3 rig/test-monitor-invariant.py`; baseline: `MESHAC_TEST_BASELINE=1 MESHAC_TEST_TREE=/dev/shm/meshac-candidate9p-integration-20261016 python3 rig/test-monitor-invariant.py`. A baseline suite nonzero status is expected because source guards are absent. Isolated ROW_RESULT output owns per-row outcomes.

| Row | Level and9q result |9p executed result / limitation |
|---|---|---|
|CONFIG_EGREGIOUS|executed-real PASS|FAIL|
|VERDICT_ORDINARY|executed-real PASS|PASS|
|VERDICT_EGREGIOUS|executed-real PASS|FAIL|
|REMOVE_KICK|executed-real PASS|FAIL|
|REMOVE_TEMPBAN|executed-real PASS|FAIL|
|REMOVE_PERMANENT|executed-real PASS|FAIL|
|ATTACK_ATTACK1211|executed-real PASS callback; real Combat trigger rig-required|FAIL|
|ATTACK_ATTACK26|executed-real PASS callback; real Combat trigger rig-required|FAIL|
|MOVEMENT_STALE_HOLD|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|MOVEMENT_IN_BLOCK|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|MOVEMENT_PATH_NOCLIP|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|MOVEMENT_BUFFERED|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|MOVEMENT_STATUS_ONLY|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|MOVEMENT_BLINK_ORIGIN|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|NOFALL_ARM_POSITION|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|NOFALL_ARM_STATUS|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|NOFALL_OLD_DEBT|source-structure-only PASS MonitorSourceWitnessTest + MonitorGate; real geometry/packet/debt trigger rig-required|Missing/conditional guards by source diff; not runtime measured|
|TOGGLE_CLEAR_ALL|source-structure-only PASS + pure SafeSwap/MonitorGate; actual registered command/load fixture rig-required|Missing guards by production diff; not runtime measured|
|RELOAD_CLEAR_ALL|source-structure-only PASS + pure SafeSwap/MonitorGate; actual registered command/load fixture rig-required|Missing guards by production diff; not runtime measured|
|CASES_AUTO_BAN|executed-real PASS|FAIL|
|CASES_STAFF_SCAN|executed-real PASS|FAIL|
|JOIN_AUTO_BAN|source-structure-only Cases.activeBan return path; actual JOIN callback rig-required|Cases lookup FAIL; actual JOIN unmeasured|
|FINGERPRINT_BRAND|executed-real PASS|FAIL|
|FINGERPRINT_CHANNEL|executed-real PASS|FAIL|
|FINGERPRINT_FORGET|executed-real PASS|FAIL|
|VEHICLE_REFUSE|executed-real PASS, qualifying detector + actual callback body, cancel/rollback spies, enforced positive|Not individually measured; baseline run stops before combined route fixtures|
|TOTEM_CONTAINER|executed-real PASS, qualifying detector + actual callback body, cancel/rollback spies, enforced positive|Not individually measured; baseline run stops before combined route fixtures|
|TOTEM_OFFHAND_SWAP|executed-real PASS, qualifying detector + actual callback body, cancel/rollback spies, enforced positive|Not individually measured; baseline run stops before combined route fixtures|
|SIGN_CANCEL|executed-real PASS, qualifying detector + actual callback body, cancel/rollback spies, enforced positive|Not individually measured; baseline run stops before combined route fixtures|
|USE_REFUSE|executed-real PASS, qualifying detector + actual callback body, cancel/rollback spies, enforced positive|Not individually measured; baseline run stops before combined route fixtures|
|ENTITY_REFUSE|executed-real PASS Interact.entity threshold + enforced positive; packet callback integration rig-required|Not individually measured|
|PLACE_REFUSE|source-structure-only enforce gates retained; real geometry/refusal/rollback witness rig-required|Conditional egregious bypass by source; not runtime measured|
|MINING_REFUSE|source-structure-only enforce gates retained; real geometry/refusal/rollback witness rig-required|Conditional egregious bypass by source; not runtime measured|
|INVENTORY_HEAT|executed-real PASS hotbar threshold witness; burst/beat + callback full row rig-required|Not individually measured|
|GLIDE_CORRECTION|executed-real PASS startFly six-start witness; energy/check correction row rig-required|Not individually measured|
|ASYNC_SIGNAL_RECHECK|executed-real PASS, qualifying detector + actual callback body, cancel/rollback spies, enforced positive|Not individually measured; baseline run stops before combined route fixtures|
|LOG_ONLY_OTHER_SIGNALS|rig-required exhaustive signal-site trigger inventory; selected inventory/glide/interact routes executed only|Not measured|

## Witness and scope details
- Both packet-family callback suites use actual AttackMixin source. No duplicate wiring was added. Forced nonnull Combat result isolates LOG-vs-cancel decision. Mapped handler signatures were separately verified in all4 game jars.
- Config scenario calls each CheckNames.ALL key plus unknown sentinel, with null/empty/mixed-case egregious lists. Verdict ordinary and egregious rows use high-weight signals with log witness. Removal seam is reflected directly; enforced controls select kick, tempban and permanent ban with history.
- Cases staff scan is an actual activeBan lookup with newer auto cases; explicit staff ban remains active. Staff command execution itself remains a separate rig control, not claimed from source lookup.
- Fingerprint brand and channel both match real check branches. Main fixture covers at0/4999/5000ms and no REMOVED entry, cleanup and enabled-mode action. Isolated baseline rows compare action behavior independently.
- Vehicle five distinct ticks, Totem impossible5ms fill via container and offhand callbacks, repeated swap-use, rapid entity changes, typed sign immediately after editor click, hotbar>30 and Glide six-start all have an observed SIGNAL witness and enforced counterpart. Actual sign lambda queued while enforcing is drained after monitor true and logs without removal. Container/use/offhand monitor callbacks assert no cancel/resync/rollback.
- No placeholder JUnit adapter or skipped row is counted green. The supplied37-row spec has stricter full branch/parameter coverage than the current fixture; remaining row portions go to the private native rig.
- Veil and dragon-egg rules remain separate feature policy. They have veilEnabled/veilXray/veilEsp and enderchestEggRule controls, documented as not governed by monitor mode.

## 9r additional row (9q remains frozen)
38. COMBATLOG_DISCONNECT: actual CombatLog class compiled with Fabric event/service doubles; tagged live-player disconnect in monitor produces a would-have-killed witness, zero kill, zero case, zero broadcast; same trigger in enforce kills once, creates one case and broadcasts once. Monitor disconnect consumes the tag. Native disconnect/event ordering remains rig-required (W14). Preexisting in production-r3 and9q, fixed in9r.

CombatLog adapter scope: it invokes the real private tag method reflectively and exercises the real registered DISCONNECT callback and tag consumption. It does not invoke or witness the actual AFTER_DAMAGE PvP tagging callback. Actual damage-event tagging, source attribution and native event ordering remain W14 rig-required.
