FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV GRADLE_USER_HOME=/root/.gradle
ENV PATH="/opt/gradle-8.10/bin:${JAVA_HOME}/bin:${PATH}"

RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        unzip openjdk-17-jdk-headless && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Gradle 8.10
RUN curl -fsSL https://services.gradle.org/distributions/gradle-8.10-bin.zip -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    ln -sf /opt/gradle-8.10/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle.zip

RUN mkdir -p /home/user && chmod 0755 /home/user

# Create the project skeleton (build.gradle, settings.gradle, source dirs).
RUN mkdir -p /home/user/myproject && \
    cd /home/user/myproject && \
    cat > settings.gradle <<'EOF'
rootProject.name = 'binary-heap-event-scheduler'
EOF

RUN cat > /home/user/myproject/build.gradle <<'EOF'
plugins {
    id 'application'
    id 'java'
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

ext {
    gdxVersion = '1.14.2'
}

dependencies {
    implementation "com.badlogicgames.gdx:gdx:${gdxVersion}"
    implementation "com.badlogicgames.gdx:gdx-backend-headless:${gdxVersion}"
    runtimeOnly    "com.badlogicgames.gdx:gdx-platform:${gdxVersion}:natives-desktop"
}

application {
    mainClass = 'com.example.scheduler.Main'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('run') {
    standardInput = System.in
}
EOF

# Generate the Gradle wrapper bundled with the project.
RUN cd /home/user/myproject && \
    gradle wrapper --gradle-version 8.10 --distribution-type bin && \
    chmod +x ./gradlew

# Pre-warm the Gradle dependency cache so `--offline` works at task time.
# A throw-away stub Main lets compileJava resolve the runtime classpath; we
# delete it (and the build outputs) immediately so the initial-state test
# still sees an unimplemented project.
RUN mkdir -p /home/user/myproject/src/main/java/com/example/scheduler && \
    cat > /home/user/myproject/src/main/java/com/example/scheduler/_Bootstrap.java <<'EOF'
package com.example.scheduler;

public final class _Bootstrap {
    public static void main(String[] args) {
        System.out.println("bootstrap");
    }
}
EOF

RUN cd /home/user/myproject && \
    ./gradlew --no-daemon --console=plain --warning-mode=none \
        compileJava resolveRuntimeClasspath 2>/dev/null || \
    ./gradlew --no-daemon --console=plain --warning-mode=none compileJava

RUN cd /home/user/myproject && \
    ./gradlew --no-daemon --console=plain --warning-mode=none \
        -q dependencies --configuration runtimeClasspath > /dev/null && \
    rm -rf /home/user/myproject/build && \
    rm -f /home/user/myproject/src/main/java/com/example/scheduler/_Bootstrap.java

# Validate that the wrapper works offline (sanity check during image build).
RUN cd /home/user/myproject && \
    ./gradlew --no-daemon --offline --console=plain --version > /dev/null && \
    ./gradlew --no-daemon --offline --console=plain --warning-mode=none \
        -q dependencies --configuration runtimeClasspath > /dev/null

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 headless backend Gradle project skeleton for the BinaryHeap event scheduler task."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - openjdk-17-jdk-headless for the JVM toolchain
#   - Gradle 8.10 installed under /opt/gradle-8.10 (also exposed on PATH)
#   - /home/user/myproject: pre-configured Gradle project with libGDX 1.14.2 + headless backend
#       * settings.gradle / build.gradle pinned to gdxVersion = 1.14.2
#       * ./gradlew wrapper (Gradle 8.10)
#       * Gradle dependency cache pre-warmed so `--offline` works
#       * src/main/java/com/example/scheduler/ is empty (no Main.java yet)
