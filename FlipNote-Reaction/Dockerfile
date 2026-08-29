FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Seoul
RUN apt-get update \
    && apt-get install -y tzdata \
    && ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/reaction-0.0.1-SNAPSHOT.jar .

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "reaction-0.0.1-SNAPSHOT.jar"]
