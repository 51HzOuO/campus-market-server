# Build the jar inside the image. The Git checkout used by CloudBase does not
# contain Maven's ignored target/ directory.
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./
COPY src src
RUN mvn -B -DskipTests package

# Use the Debian-based runtime so the OS CA bundle and Java cacerts are
# maintained together. The Alpine image in the previous deployment could not
# build a trust path for api.weixin.qq.com and caused SunCertPathBuilderException.
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates ca-certificates-java \
    && update-ca-certificates \
    && test -s /etc/ssl/certs/java/cacerts \
    && rm -rf /var/lib/apt/lists/*
# Explicitly select the trust store refreshed above. This avoids relying on the
# JRE image's bundled cacerts symlink, which can be stale in managed builders.
ENV JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts -Djavax.net.ssl.trustStorePassword=changeit"
WORKDIR /app
COPY --from=builder /build/target/campus-market-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
