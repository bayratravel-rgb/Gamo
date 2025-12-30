# Stage 1: Build
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Use the correct task name provided by the shadow plugin
RUN gradle shadowJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/ktor-server.jar /app/ktor-server.jar
ENTRYPOINT ["java","-jar","/app/ktor-server.jar"]
