# Principles

Write the least code that does the job. Before adding anything, go down this list and stop at the first one that works:

1. Does it need to exist? If not, skip it.
2. Is it already in this codebase? Reuse it.
3. Does the Java standard library do it?
4. Does the game or Fabric already do it? Use that.
5. Is it a dependency we already have?
6. Can it be one line?
7. Only then write the smallest thing that works.

Read the code you are about to touch and follow the real flow before picking a step. Be lazy about the solution, never about reading.

Small does not mean golfed, and it never means skipping validation, error handling or security. An anticheat that trusts what the client sends is not an anticheat.

For meshac that means: use the game's own movement and collision code instead of copying physics, no dependencies beyond Fabric, the smallest check that catches each cheat, no features nobody asked for. A check only ships with a test that shows it catching the cheat and a test that shows it leaving normal play alone.
