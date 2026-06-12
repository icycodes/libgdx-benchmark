FROM ubuntu:24.04

# Base system tools, JDK, and Python tooling required by the verifier.
RUN apt-get update -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        unzip zip openjdk-21-jdk-headless && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Gradle 8.10 so we can bootstrap the wrapper for the libGDX project.
RUN wget -q https://services.gradle.org/distributions/gradle-8.10-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    ln -s /opt/gradle-8.10/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle.zip

# Standard /home/user layout.
RUN mkdir -p /home/user && chmod 0755 /home/user

# Bootstrap the libGDX project skeleton with a Gradle wrapper and warm
# the wrapper distribution so /home/user/myproject/gradlew works offline.
RUN mkdir -p /home/user/myproject && \
    cd /home/user/myproject && \
    gradle wrapper --gradle-version 8.10 --distribution-type bin && \
    ./gradlew --no-daemon --version

# Pre-warm the Gradle dependency cache with libGDX 1.14.2 + shadow plugin
# so the agent's first build does not need to download heavy artifacts.
RUN mkdir -p /tmp/cache_warm
RUN cat > /tmp/cache_warm/settings.gradle <<'EOF'
rootProject.name = 'cache-warm'
EOF
RUN cat > /tmp/cache_warm/build.gradle <<'EOF'
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '8.3.5'
}
repositories { mavenCentral() }
dependencies {
    implementation 'com.badlogicgames.gdx:gdx:1.14.2'
    implementation 'com.badlogicgames.gdx:gdx-backend-headless:1.14.2'
    runtimeOnly    'com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop'
}
EOF
RUN cd /tmp/cache_warm && \
    gradle --no-daemon dependencies --configuration runtimeClasspath && \
    gradle --no-daemon tasks --all > /dev/null && \
    rm -rf /tmp/cache_warm

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 headless project skeleton with Gradle 8.10 wrapper pre-bootstrapped for the InputMultiplexer handler chain task."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - openjdk-21-jdk-headless: JDK for building and running the libGDX headless app
#   - Gradle 8.10 (installed under /opt/gradle-8.10, linked to /usr/local/bin/gradle)
#   - /home/user/myproject: empty libGDX project skeleton with the Gradle wrapper pre-installed
#   - Warmed Gradle cache containing libGDX 1.14.2 (core + headless + natives-desktop) and com.gradleup.shadow 8.3.5
