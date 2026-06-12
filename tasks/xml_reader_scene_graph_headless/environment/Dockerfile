FROM ubuntu:24.04

ENV GRADLE_VERSION=8.10
ENV LIBGDX_VERSION=1.14.2
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV GRADLE_USER_HOME=/root/.gradle
ENV PATH=$JAVA_HOME/bin:$PATH

RUN apt-get update -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        unzip openjdk-17-jdk-headless && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
        -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle.zip

RUN mkdir -p /home/user && chmod 0755 /home/user

# Create the libGDX project skeleton (Gradle multi-module layout).
RUN mkdir -p /home/user/gdx-scene-loader/app/src/main/java/com/example/scene

RUN cat > /home/user/gdx-scene-loader/settings.gradle <<'EOF'
rootProject.name = 'gdx-scene-loader'
include 'app'
EOF

RUN cat > /home/user/gdx-scene-loader/build.gradle <<'EOF'
// Root project: shared configuration for all sub-modules.
allprojects {
    repositories {
        mavenCentral()
    }
}
EOF

RUN cat > /home/user/gdx-scene-loader/app/build.gradle <<'EOF'
plugins {
    id 'application'
    id 'java'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    // The executor implements the real Main class; this default points at the
    // stub provided by the environment skeleton.
    mainClass = 'com.example.scene.Main'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.badlogicgames.gdx:gdx:1.14.2'
    implementation 'com.badlogicgames.gdx:gdx-backend-headless:1.14.2'
    runtimeOnly    'com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop'
}

tasks.named('run') {
    standardInput = System.in
}
EOF

# Stub Main.java so the project compiles before the executor implements it.
RUN cat > /home/user/gdx-scene-loader/app/src/main/java/com/example/scene/Main.java <<'EOF'
package com.example.scene;

public class Main {
    public static void main(String[] args) {
        throw new UnsupportedOperationException(
            "Executor must implement Main.main with the headless libGDX scene loader."
        );
    }
}
EOF

# Initialise the Gradle wrapper inside the project so the executor uses
# ./gradlew without any external Gradle installation, then pre-warm the
# dependency cache for libGDX so verification can run offline-friendly.
RUN cd /home/user/gdx-scene-loader && \
    gradle wrapper --gradle-version ${GRADLE_VERSION} --distribution-type bin && \
    chmod +x gradlew && \
    ./gradlew --no-daemon :app:classes --quiet && \
    ./gradlew --no-daemon :app:dependencies --quiet > /dev/null

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 headless task scaffold for the XML scene-graph loader, built on Ubuntu 24.04 with OpenJDK 17 and a pre-warmed Gradle 8.10 wrapper."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - OpenJDK 17 (headless) for compiling and running the libGDX project
#   - Gradle 8.10 distribution at /opt/gradle-8.10 plus the wrapper inside the project
#   - libGDX 1.14.2 core + headless backend + desktop natives pre-fetched into the Gradle cache
#   - /home/user/gdx-scene-loader: project root with the Gradle wrapper, settings.gradle, root build.gradle
#   - /home/user/gdx-scene-loader/app: application sub-module with build.gradle and a stub Main.java
