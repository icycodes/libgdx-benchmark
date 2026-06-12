FROM ubuntu:24.04

ENV GRADLE_VERSION=8.10
ENV GRADLE_HOME=/opt/gradle
ENV PATH=/opt/gradle/bin:/usr/lib/jvm/java-17-openjdk-amd64/bin:${PATH}
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV GRADLE_USER_HOME=/root/.gradle

RUN apt-get update -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep \
        openjdk-17-jdk-headless unzip zip && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    pip3 install --break-system-packages pytest-json-ctrf && \
    curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    mv "/opt/gradle-${GRADLE_VERSION}" /opt/gradle && \
    rm /tmp/gradle.zip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /home/user && chmod 0755 /home/user
RUN mkdir -p /home/user/gdx-spline-sampler && chmod 0755 /home/user/gdx-spline-sampler

LABEL Author="generate-harbor-tasks"
LABEL Description="libGDX 1.14.2 headless backend task environment with OpenJDK 17 and Gradle 8.10 for the catmull_spline_path_sampler_headless task."

CMD ["/bin/bash", "-c", "trap : TERM INT; sleep infinity & wait"]

# Container contents:
#   - always required apt packages: curl wget ca-certificates git python3 python3-pip python3-pytest bash ripgrep
#   - always required pip3 packages: pytest-json-ctrf
#   - OpenJDK 17 headless JDK for compiling and running the libGDX project
#   - Gradle 8.10 binary distribution installed to /opt/gradle (on PATH)
#   - /home/user/gdx-spline-sampler: empty project directory where the executor will scaffold the libGDX headless sampler
