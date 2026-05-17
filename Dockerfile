# Amazon Corretto 17 베이스 이미지
FROM amazoncorretto:17

# 작업 디렉토리를 /app 으로 설정
WORKDIR /app

# 프로젝트 파일 전체를 컨테이너로 복사
# (.dockerignore에 적힌 파일들은 자동으로 제외됨)
COPY . .

# gradlew 실행 권한 부여 후 빌드 (테스트 스킵)
RUN chmod +x ./gradlew && \
    ./gradlew bootJar --no-daemon -x test

# 80 포트 노출 (application-prod.yaml의 server.port와 동일하게)
EXPOSE 80

# jar 파일 이름 추론에 사용할 환경변수
ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=1.2-M8

# JVM 옵션 (기본값: 빈 문자열)
ENV JVM_OPTS=""

# 애플리케이션 실행
# sh -c 형태여야 $JVM_OPTS, $PROJECT_NAME 같은 환경변수가 런타임에 실제 값으로 치환됨
CMD ["sh", "-c", "java $JVM_OPTS -jar /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar"]