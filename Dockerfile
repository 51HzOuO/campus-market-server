# Build the jar inside the image. The Git checkout used by CloudBase does not
# contain Maven's ignored target/ directory.
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/campus-market-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
