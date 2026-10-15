# syntax=docker/dockerfile:1

FROM eclipse-temurin:22-jdk-alpine AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:22-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/qlhs-api-*.jar app.jar

RUN addgroup -S spring && adduser -S -G spring spring \
    && chown spring:spring app.jar
USER spring:spring

ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
