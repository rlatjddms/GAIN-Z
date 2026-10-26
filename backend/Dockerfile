# GAIN-Z 백엔드 이미지 (#176)
# 1단계: Gradle로 실행 jar를 만든다
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성만 먼저 받아 두면 소스가 바뀌어도 이 레이어는 캐시된다
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
# Windows에서 받은 gradlew가 CRLF면 실행이 안 되므로 LF로 맞춘다
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && (./gradlew dependencies --no-daemon -q > /dev/null 2>&1 || true)

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# 2단계: JRE만 있는 실행 이미지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 운영 프로필. 나머지 값은 환경 변수로 넣는다 (docs/setup.md "운영 실행" 표)
ENV SPRING_PROFILES_ACTIVE=prod \
    UPLOAD_DIR=/data/uploads \
    TZ=Asia/Seoul
VOLUME /data/uploads
EXPOSE 8080

# 서버가 떴는지 포트로만 확인한다 (actuator 없음)
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
