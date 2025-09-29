# =============================================================================
# 보안 강화된 Booking Service Dockerfile
# =============================================================================

# 1. 빌드 스테이지 (보안 강화)
FROM eclipse-temurin:17-jdk-alpine AS builder

# 보안 업데이트 및 취약점 패치
RUN apk update && \
    apk upgrade && \
    apk add --no-cache curl

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# 2. 실행 스테이지 (보안 강화)
FROM eclipse-temurin:17-jre-alpine

# 보안 업데이트 및 취약점 패치
RUN apk update && \
    apk upgrade && \
    apk add --no-cache curl

# 비루트 사용자 생성
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser -s /bin/false appuser

# 애플리케이션 디렉토리 생성 및 권한 설정
RUN mkdir -p /app && \
    chown -R appuser:appuser /app

# 애플리케이션 파일 복사
COPY --from=builder /app/build/libs/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

# Spring Boot용 디렉토리 미리 생성 (읽기 전용 파일시스템 대비)
RUN mkdir -p /app/tmp /app/tomcat && \
    chown -R appuser:appuser /app && \
    chmod -R 777 /app/tmp /app/tomcat

# 비루트 사용자로 전환
USER appuser

# 작업 디렉토리 설정
WORKDIR /app

# 포트 노출
EXPOSE 8080

# 헬스체크 설정 (보안 강화)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]