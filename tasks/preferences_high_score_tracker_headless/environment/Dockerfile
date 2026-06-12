FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV GRADLE_USER_HOME=/root/.gradle
ENV PATH=/opt/gradle/bin:$PATH
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Core OS + Python tooling + JDK for Gradle/libGDX builds.
RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        unzip openjdk-17-jdk-headless && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Gradle 8.10 system-wide so we can bootstrap the wrapper without internet at task time.
RUN curl -fsSL https://services.gradle.org/distributions/gradle-8.10-bin.zip -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    mv /opt/gradle-8.10 /opt/gradle && \
    rm /tmp/gradle.zip && \
    gradle --version

RUN mkdir -p /home/user && chmod 0755 /home/user

# Create the project skeleton and bootstrap the Gradle wrapper (pinned at 8.10).
# Then prime the Gradle wrapper distribution cache so `./gradlew --offline` works.
RUN mkdir -p /home/user/highscores && \
    cd /home/user/highscores && \
    gradle wrapper --gradle-version 8.10 --distribution-type bin && \
    ./gradlew --no-daemon --version

# Pre-fetch the libGDX 1.14.2 headless dependency graph into the Gradle module cache,
# so the executor can build the project with `--offline`.
RUN mkdir -p /tmp/prefetch

RUN cat > /tmp/prefetch/settings.gradle <<'EOF'
rootProject.name = 'prefetch'
EOF

RUN cat > /tmp/prefetch/build.gradle <<'EOF'
plugins {
    id 'java'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.badlogicgames.gdx:gdx:1.14.2'
    implementation 'com.badlogicgames.gdx:gdx-backend-headless:1.14.2'
    runtimeOnly 'com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop'
}

tasks.register('resolveAll') {
    doLast {
        configurations.each { c ->
            if (c.canBeResolved) {
                c.resolve()
            }
        }
    }
}
EOF

RUN cd /tmp/prefetch && \
    gradle --no-daemon resolveAll && \
    cd / && \
    rm -rf /tmp/prefetch

# Provide an initial events.txt so the initial-state test sees the input file.
# The verifier overwrites this file with the canonical scenario before evaluation.
RUN cat > /home/user/highscores/events.txt <<'EOF'
# Placeholder events file — replaced by the verifier before evaluation.
DUMP
EOF

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 headless backend Preferences task with Gradle wrapper and pre-populated dependency cache."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - OpenJDK 17 (headless) + Gradle 8.10 installed under /opt/gradle
#   - /home/user/highscores: project root with Gradle wrapper (gradle-wrapper.jar at 8.10) primed for offline use
#   - /root/.gradle: Gradle user home with the libGDX 1.14.2 dependency graph (gdx, gdx-backend-headless, gdx-platform natives-desktop) pre-cached so `./gradlew --offline` succeeds
#   - /home/user/highscores/events.txt: placeholder input file overwritten during verification
