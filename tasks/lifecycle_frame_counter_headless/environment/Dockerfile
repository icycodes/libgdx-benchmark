FROM ubuntu:24.04

ENV GRADLE_VERSION=8.10
ENV GRADLE_HOME=/opt/gradle
ENV PATH=/opt/gradle/bin:${PATH}
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

RUN apt-get update -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        unzip openjdk-17-jdk && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    mv "/opt/gradle-${GRADLE_VERSION}" /opt/gradle && \
    rm /tmp/gradle.zip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /home/user && chmod 0755 /home/user
RUN mkdir -p /home/user/myproject

# Pre-warm the Gradle/Maven cache with the libGDX headless artifacts so the
# executor's first build does not have to download everything from scratch.
RUN mkdir -p /tmp/gdx-warmup/src/main/java/warmup && \
    cd /tmp/gdx-warmup && \
    cat > settings.gradle <<'EOF'
rootProject.name = 'gdx-warmup'
EOF
RUN cat > /tmp/gdx-warmup/build.gradle <<'EOF'
plugins {
    id 'java'
    id 'application'
}
repositories {
    mavenCentral()
}
dependencies {
    implementation 'com.badlogicgames.gdx:gdx:1.14.2'
    implementation 'com.badlogicgames.gdx:gdx-backend-headless:1.14.2'
    runtimeOnly    'com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop'
}
application {
    mainClass = 'warmup.Main'
}
EOF
RUN cat > /tmp/gdx-warmup/src/main/java/warmup/Main.java <<'EOF'
package warmup;

public class Main {
    public static void main(String[] args) {
        System.out.println("warmup ok");
    }
}
EOF
RUN cd /tmp/gdx-warmup && \
    gradle --no-daemon --quiet build && \
    gradle --no-daemon --quiet run && \
    rm -rf /tmp/gdx-warmup

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX headless lifecycle task: Ubuntu 24.04 with OpenJDK 17, Gradle 8.10, and a warmed Maven cache for com.badlogicgames.gdx:1.14.2."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - openjdk-17-jdk for compiling and running libGDX Java applications
#   - Gradle 8.10 installed under /opt/gradle (added to PATH)
#   - /home/user/myproject: empty project root the executor will populate
#   - Pre-warmed Maven Central cache containing com.badlogicgames.gdx:gdx:1.14.2,
#     com.badlogicgames.gdx:gdx-backend-headless:1.14.2, and
#     com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop, so the first
#     Gradle build in the project does not have to download them.
