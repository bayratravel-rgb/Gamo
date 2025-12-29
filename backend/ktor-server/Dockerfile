FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
EXPOSE 8080
# FIXED: Added RUN
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/ktor-server.jar /app/ktor-server.jar
ENTRYPOINT ["java","-jar","/app/ktor-server.jar"]
