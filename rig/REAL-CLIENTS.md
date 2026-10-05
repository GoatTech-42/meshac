# Real hacked client testing (plan, checked 2026-10-05)
Sources (official repos only):
- Wurst7 (GPL-3.0): github.com/Wurst-Imperium/Wurst7, branches 26.1.2, 26.2, 26.3 exist. 26.1.2 matches the rig.
- Meteor Client: github.com/MeteorDevelopment/meteor-client, master targets MC 26.2 (Fabric loader 0.19.3, Java 25). Needs a 26.2 rig too.
Method: Docker image with Java 25, Xvfb and software GL (Mesa llvmpipe). Build from source in a no-network step, scan the tree first (no unexpected network or exec code), then run the client against meshac-rig on meshac-net with offline login, memory/CPU caps. Drive hacks by chat commands (Wurst .fly, .speed, .nofall, .timer...) and log meshac flags per hack.
Status: not built yet. Needs one internet-enabled build step (Gradle, MC assets), then runtime on the internal network.
