# 1. 빌드 스테이지
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# 2. 실행 스테이지
FROM openjdk:17-jdk-slim
COPY --from=builder /app/build/libs/*.jar /app.jar

# Expose port
EXPOSE 8080

# 3. 헬스 체크 설정 추가
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -sf http://localhost:8080/health || exit 1

# 4. 컨테이너 실행 명령어
ENTRYPOINT ["java", "-jar", "/app.jar"]