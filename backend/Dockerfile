# 빌드 스테이지 - Maven 캐시를 이미지에 남기지 않기 위해 최종 이미지와 분리한다.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# 의존성만 먼저 받아서 레이어 캐싱 - pom.xml이 안 바뀌면 소스만 바뀌어도 이 레이어는 재사용된다.
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# 실행 스테이지 - JDK가 아니라 JRE만 있으면 되고, Maven/소스는 이미지에 남기지 않는다.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# DB_HOST 등은 GitLab CI가 EC2에서 docker run/compose 실행 시 환경변수로 주입한다 (application.yml 기본 패턴과 동일).
ENTRYPOINT ["java", "-jar", "app.jar"]
