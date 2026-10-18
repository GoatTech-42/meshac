FROM eclipse-temurin:25-jdk
RUN apt-get update && apt-get install -y --no-install-recommends xvfb xdotool imagemagick libgl1-mesa-dri libglx-mesa0 libgl1 libxrandr2 libxcursor1 libxxf86vm1 libxi6 libxtst6 libopenal1 libpulse0 x11-utils git ca-certificates && rm -rf /var/lib/apt/lists/*
RUN useradd -m mc
USER mc
WORKDIR /home/mc
