FROM ubuntu:24.04

# Base tooling + OpenJDK 17 for libGDX 1.14.2.
RUN apt-get update -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        openjdk-17-jdk unzip xz-utils && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Gradle 8.10 system-wide (matches the version recommended by the libGDX
# project-generation docs for libGDX 1.14.2).
ENV GRADLE_VERSION=8.10
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH="${GRADLE_HOME}/bin:${PATH}"
RUN mkdir -p /opt/gradle && \
    curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt/gradle && \
    rm /tmp/gradle.zip && \
    gradle --version

# Pre-warm the Gradle dependency cache with libGDX 1.14.2 so that the executor
# (and the verifier) can run `./gradlew --offline build` and `run`.
RUN mkdir -p /tmp/gdx-cache-seed/src/main/java/seed && \
    cat >/tmp/gdx-cache-seed/settings.gradle <<'EOF'
rootProject.name = 'gdx-cache-seed'
EOF

RUN cat >/tmp/gdx-cache-seed/build.gradle <<'EOF'
plugins {
    id 'application'
}

repositories {
    mavenCentral()
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
    mainClass = 'seed.Seed'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
EOF

RUN cat >/tmp/gdx-cache-seed/src/main/java/seed/Seed.java <<'EOF'
package seed;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.Animation;

public class Seed extends ApplicationAdapter {
    @Override public void create() {
        Animation<Integer> a = new Animation<Integer>(1.0f, 1, 2, 3);
        a.setPlayMode(Animation.PlayMode.LOOP);
        System.out.println(a.getKeyFrame(0.0f));
        Gdx.app.exit();
    }

    public static void main(String[] args) {
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        cfg.updatesPerSecond = 0;
        new HeadlessApplication(new Seed(), cfg);
    }
}
EOF

RUN cd /tmp/gdx-cache-seed && \
    gradle --no-daemon build && \
    gradle --no-daemon run && \
    gradle --no-daemon --stop || true

# Workspace for the executor. Left intentionally empty; the executor creates
# the Gradle project structure from scratch as part of the task.
RUN mkdir -p /home/user && chmod 0755 /home/user && \
    mkdir -p /home/user/myproject

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 + Gradle 8.10 environment with the libGDX dependency cache pre-warmed for offline headless animation sampler tasks."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - OpenJDK 17 (JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64)
#   - Gradle 8.10 installed at /opt/gradle/gradle-8.10
#   - Gradle / Maven dependency cache pre-populated with libGDX 1.14.2 (gdx,
#     gdx-backend-headless, gdx-platform:natives-desktop) under /root/.gradle
#   - /home/user/myproject: empty workspace for the executor to populate
