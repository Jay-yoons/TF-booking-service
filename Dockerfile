# 1. 빌드 스테이지
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# 2. 실행 스테이지
FROM openjdk:17-jre-slim
COPY --from=builder /app/build/libs/*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]