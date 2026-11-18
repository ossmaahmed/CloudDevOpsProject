FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/roadmap-service-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
