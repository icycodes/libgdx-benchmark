FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV GRADLE_VERSION=8.10
ENV GRADLE_USER_HOME=/root/.gradle

RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        unzip openjdk-17-jdk-headless && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Gradle 8.10 (recommended in plan.md for libGDX 1.14.2).
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip && \
    ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle

RUN mkdir -p /home/user && chmod 0755 /home/user

# Pre-warm Gradle distribution + Maven Central caches with the libGDX 1.14.2
# coordinates the task uses (gdx, gdx-backend-headless, gdx-platform natives-desktop).
# This makes the executor's `./gradlew` and Maven downloads fast or fully offline.
RUN mkdir -p /opt/gdx-cache-warm && cd /opt/gdx-cache-warm && \
    printf '%s\n' "rootProject.name = 'gdx-cache-warm'" > settings.gradle && \
    printf '%s\n' \
        "plugins { id 'java-library' }" \
        "repositories { mavenCentral() }" \
        "dependencies {" \
        "    api 'com.badlogicgames.gdx:gdx:1.14.2'" \
        "    api 'com.badlogicgames.gdx:gdx-backend-headless:1.14.2'" \
        "    runtimeOnly 'com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop'" \
        "}" > build.gradle && \
    gradle --no-daemon wrapper --gradle-version "${GRADLE_VERSION}" --distribution-type bin && \
    ./gradlew --no-daemon -q dependencies --configuration runtimeClasspath > /dev/null && \
    cd / && rm -rf /opt/gdx-cache-warm

# The task itself targets this project root; create it empty so the executor
# scaffolds the multi-module Gradle layout in place.
RUN mkdir -p /home/user/match-replay-server && chmod 0755 /home/user/match-replay-server

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 headless-backend task environment (JDK 17 + Gradle 8.10) with pre-warmed Maven Central + Gradle wrapper caches at /root/.gradle."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - OpenJDK 17 (headless) for compiling and running the libGDX project
#   - Gradle 8.10 installed at /opt/gradle-8.10 (symlinked as `gradle` on PATH)
#   - Pre-warmed Gradle caches under /root/.gradle:
#       * Gradle 8.10 wrapper distribution
#       * Maven Central artifacts for gdx, gdx-backend-headless, gdx-platform natives-desktop (libGDX 1.14.2)
#   - /home/user/match-replay-server: empty project root for the executor to scaffold
