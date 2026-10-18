# Testing against real hacked clients

Only official repos, read before running, built in a container, run with no internet.

- Wurst: github.com/Wurst-Imperium/Wurst7, branch 26.1.2 (GPL-3.0). Working now.
- Meteor Client: github.com/MeteorDevelopment/meteor-client. Its master targets 26.2, so it needs a 26.2 server. Not set up yet.

How it works: a Docker image with Java 25, a virtual display and software rendering. Gradle downloads everything once with internet on. After that `client/run-client.sh` starts the client on the isolated network, and `client/drive.sh <hack>` toggles a hack through chat, holds forward and jump, and reports what meshac did and how fast.

Two things bit us along the way. Wurst remembers enabled hacks between runs, so the scripts reset that file. And player data is saved by name, so every run uses a fresh name.
