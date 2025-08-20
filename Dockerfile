# 1. 빌드 스테이지
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY . .
# gradlew 파일에 실행 권한을 부여합니다.
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# 2. 실행 스테이지
FROM openjdk:17-jdk-slim
COPY --from=builder /app/build/libs/*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]