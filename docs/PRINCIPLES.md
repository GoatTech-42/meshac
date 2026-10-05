# Principles

Write only what the task needs. Before adding code, stop at the first rung that holds:

1. Does this need to exist? If not, skip it.
2. Is it already in this codebase? Reuse it.
3. Does the Java standard library do it?
4. Does the game or Fabric API already do it? Use the game.
5. Is it an installed dependency? Use that.
6. Is it one line?
7. Only then: the minimum that works.

Read the code a change touches and trace the real flow before choosing a rung. Lazy about the solution, never about reading.

Small because necessary, not golfed. Never cut validation of client input, error handling, security or data-loss handling. An anticheat that trusts packets is not one.

For meshac this means: reuse the game own movement and collision code for prediction instead of copying physics, no dependencies beyond Fabric, the smallest check that catches each cheat, no speculative features. A check ships only with a test that shows it catching the cheat and a test that shows it leaves legit play alone.
