FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app

# 配置阿里云 Maven 镜像
RUN mkdir -p /root/.m2
RUN echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyunmaven</id><mirrorOf>*</mirrorOf><name>阿里云公共仓库</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/campus-market-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
