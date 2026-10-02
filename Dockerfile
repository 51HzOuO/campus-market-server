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
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/campus-market-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
