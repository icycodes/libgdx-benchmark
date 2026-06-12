FROM ubuntu:24.04

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV GRADLE_VERSION=8.10
ENV GRADLE_HOME=/opt/gradle/gradle-8.10
ENV PATH=$JAVA_HOME/bin:$GRADLE_HOME/bin:$PATH

RUN apt-get update -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        openjdk-17-jdk-headless unzip xz-utils && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Gradle 8.10 (required by the research plan's non-interactive setup commands).
RUN mkdir -p /opt/gradle && \
    curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt/gradle && \
    rm /tmp/gradle.zip && \
    ln -s "${GRADLE_HOME}/bin/gradle" /usr/local/bin/gradle

# Warm up the Gradle/libGDX dependency cache so headless:run inside the
# task can resolve gdx 1.14.2 + gdx-backend-headless 1.14.2 + gdx-platform
# natives without depending on a fast network connection during evaluation.
RUN mkdir -p /tmp/gdxwarmup/src/main/java/warmup && \
    cd /tmp/gdxwarmup && \
    cat >settings.gradle <<'EOF'
rootProject.name = 'gdxwarmup'
EOF
RUN cd /tmp/gdxwarmup && \
    cat >build.gradle <<'EOF'
plugins { id 'java' }
repositories { mavenCentral() }
dependencies {
    implementation 'com.badlogicgames.gdx:gdx:1.14.2'
    implementation 'com.badlogicgames.gdx:gdx-backend-headless:1.14.2'
    runtimeOnly    'com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop'
}
EOF
RUN cd /tmp/gdxwarmup && \
    cat >src/main/java/warmup/Main.java <<'EOF'
package warmup;
public class Main { public static void main(String[] args) {} }
EOF
RUN cd /tmp/gdxwarmup && \
    gradle --no-daemon --console=plain build && \
    cd / && \
    rm -rf /tmp/gdxwarmup

RUN mkdir -p /home/user && chmod 0755 /home/user

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 headless task environment with JDK 17, Gradle 8.10, and a pre-warmed Maven Central cache for the headless backend."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - openjdk-17-jdk-headless: JDK for compiling and running libGDX Java code
#   - Gradle 8.10 installed at /opt/gradle, accessible globally via /usr/local/bin/gradle
#   - Pre-warmed Gradle dependency cache containing gdx:1.14.2, gdx-backend-headless:1.14.2,
#     and gdx-platform:1.14.2:natives-desktop, so the executor's `./gradlew build` does not
#     need to re-download artifacts from Maven Central during evaluation
#   - /home/user: working directory for the executor (the libGDX project goes under /home/user/myproject)
