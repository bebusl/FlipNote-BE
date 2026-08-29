FROM gradle:8-jdk17 AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
COPY src ./src

RUN ./gradlew generateProto --no-daemon
RUN ./gradlew bootJar --no-daemon

FROM amazoncorretto:17.0.17-alpine3.22
WORKDIR /app

ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata \
    && ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8082 9092

ENTRYPOINT ["java", "-jar", "app.jar"]
