# meshac punishment ladder - design draft (not yet implemented)

Goal: a legit player never reaches a kick; a cheater is removed fast; repeat cheaters climb a fair ladder; every number is a config knob.

## 1. Evidence, not counts: weights from likelihood ratios
Each flag is evidence that the player is cheating. Weight of a flag from check c:

    w_c = log2( P(flag | cheat) / P(flag | legit) )     (bits of evidence)

- P(flag | cheat) is measured on the rig with real hacked clients (hit rate per second of cheating).
- P(flag | legit) is the legit-control false-flag rate per hour, bounded by the rule of three: n clean hours means the rate is <= 3/n per hour.
- Tiers (rounded, so configs stay readable): A = near certain (fly, noclip, reach over 6, speed over 2x): w = 3. B = strong (reach 3.6-6, velocity, scaffold chain, step): w = 2. C = statistical or lag-sensitive (timer, interact burst, look, hotbar rate, click patterns): w = 1.
- A weight is lowered one tier automatically while the player's connection is bad (ping spike or burst arrival seen in the last 5 s). This is the lag-tolerance rule that the live tunnel tests showed we need.

## 2. Heat with exponential decay (replaces the 10 s hard reset)
    H(t) = H(t0) * 2^( -(t - t0) / T_half ),    H += w at each flag      (default T_half = 8 s)
Hold at H >= 3, remove at H >= R (default R = 6 bits). Setback below that.

## 3. Why a legit player cannot reach R
If a legit player trips flags as a Poisson process with rate L per second (summed over checks, weight-weighted), reaching R inside the decay window needs about k = R / w_avg flags inside about 2 * T_half. P(legit removal) per window = P(Poisson(mu) >= k), mu = L * 2 * T_half. With L from the controls (<= 3 per 60 clean hours per check, 10 checks): mu = 0.0003 * 16 -> 0.005, k = 6 for tier C: P ~ mu^k / k! ~ 1e-17 per window. The live false positives were NOT independent: one lag stall tripped several checks at once. So add the independence rule:

    Remove requires heat >= R from >= 3 independent causes, or >= 2 independent tier-A causes.
A cause = a burst: flags within 150 ms of the first flag of the burst are one cause and only the heaviest adds heat. A lag stall is one burst however many checks it trips; a hack that keeps flagging (pure fly, pure speed, killaura) makes a new cause every 150 ms and is removed in about 1 s (tier A) to 2.5 s (tier C) even though only one check fires.

## 4. Offence ladder with decaying memory
Each removal is an offence with weight 1, forgiven by half-life tau (default 14 days): effective offences n = sum 0.5^(age/tau).
Rung r = floor(n) + 1:
- r = 1: kick (the screen names the check; case id logged).
- r = 2..: tempban D_r = D_1 * m^(r-2), default D_1 = 10 min, m = 6 -> 10 min, 1 h, 6 h, 36 h, capped at D_max = 7 days.
- Permanent only when permanentBan is on AND the removal was tier A confirmed (>= 2 tier A flags) AND r >= 4. Default permanentBan = false.
- Escalation shortcut: a removal driven by tier A evidence skips one rung (fly/noclip/killaura are never accidental); tier C-only removals never skip.
- Pardon of a case removes it from n; unban keeps it.

## 5. Config (all in meshac.json, with presets lenient / default / strict)
halfLifeSeconds, holdAt, removeAt, tierWeights {A,B,C}, requireFamilies (1 or 2), kicksBeforeTempban, tempbanBaseMinutes, tempbanGrowth, tempbanMaxMinutes, offenceHalfLifeDays, permanentBan, permanentNeedsTierA, skipRungOnTierA.

## 6. Proof plan
1. Replay every legit control run and every live-session log through the new heat model offline: assert zero removals.
2. Replay every hacked-client run: assert removal times (target: fly/noclip <= 3 s, speed/killaura <= 5 s, scaffold <= 6 s).
3. Monte Carlo of the Poisson model with clustered flags (a burst of b flags from one cause) to show the families rule keeps P(legit removal) under 1e-6 per session.
4. Unit test of the ladder table for each preset.
