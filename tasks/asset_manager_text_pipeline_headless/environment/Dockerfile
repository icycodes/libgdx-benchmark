FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV GRADLE_VERSION=8.10
ENV GRADLE_USER_HOME=/root/.gradle

RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        openjdk-17-jdk-headless unzip && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install a system-level Gradle so we can bootstrap the wrapper non-interactively.
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip && \
    ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle

RUN mkdir -p /home/user && chmod 0755 /home/user

# libGDX 1.14.2 project skeleton at /home/user/myproject
RUN mkdir -p /home/user/myproject/core/src/main/java/com/example/game && \
    mkdir -p /home/user/myproject/headless/src/main/java/com/example/game/headless && \
    mkdir -p /home/user/myproject/assets

RUN cat >/home/user/myproject/settings.gradle <<'EOF'
rootProject.name = 'my-gdx-game'
include 'core'
include 'headless'
EOF

RUN cat >/home/user/myproject/build.gradle <<'EOF'
allprojects {
    apply plugin: 'eclipse'
    apply plugin: 'idea'
}

ext {
    gdxVersion = '1.14.2'
}

configure(subprojects) {
    apply plugin: 'java-library'
    sourceCompatibility = 17
    targetCompatibility = 17
    repositories {
        mavenCentral()
    }
    compileJava.options.encoding = 'UTF-8'
}
EOF

RUN cat >/home/user/myproject/core/build.gradle <<'EOF'
dependencies {
    api "com.badlogicgames.gdx:gdx:${gdxVersion}"
}
EOF

RUN cat >/home/user/myproject/headless/build.gradle <<'EOF'
apply plugin: 'application'

application {
    mainClass = 'com.example.game.headless.HeadlessLauncher'
}

dependencies {
    implementation project(':core')
    implementation "com.badlogicgames.gdx:gdx-backend-headless:${gdxVersion}"
    runtimeOnly "com.badlogicgames.gdx:gdx-platform:${gdxVersion}:natives-desktop"
}
EOF

# Stub launcher so the skeleton compiles and `./gradlew build` can prime the offline
# dependency cache. Agents are expected to replace this file with the real implementation.
RUN cat >/home/user/myproject/headless/src/main/java/com/example/game/headless/HeadlessLauncher.java <<'EOF'
package com.example.game.headless;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class HeadlessLauncher {
    public static void main(String[] args) {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new ApplicationAdapter() {
            @Override public void create() { Gdx.app.exit(); }
        }, config);
    }
}
EOF

# Bootstrap the Gradle wrapper, then run a one-off build to populate the wrapper and
# dependency caches so subsequent invocations succeed under `--offline`.
RUN cd /home/user/myproject && \
    gradle --no-daemon wrapper --gradle-version ${GRADLE_VERSION} --distribution-type bin && \
    ./gradlew --no-daemon build && \
    ./gradlew --no-daemon --stop || true

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 headless-backend project skeleton with primed Gradle offline cache."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep openjdk-17-jdk-headless unzip
#   - pip packages: pytest-json-ctrf
#   - Gradle 8.10 distribution at /opt/gradle-8.10
#   - /home/user/myproject: libGDX 1.14.2 skeleton with core/ + headless/ subprojects, Gradle wrapper, and primed offline caches
#   - Stub HeadlessLauncher at /home/user/myproject/headless/src/main/java/com/example/game/headless/HeadlessLauncher.java (agents must replace it)
