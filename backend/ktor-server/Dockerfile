FROM gradle:7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Fix: Ensure build task exists or use assemble
RUN gradle build --no-daemon -x test

FROM openjdk:17
EXPOSE 8080
# FIX: Use RUN for shell commands
RUN mkdir /app
# FIX: Copy the jar correctly (Assuming 'build/libs' contains the jar)
COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-server.jar
ENTRYPOINT ["java","-jar","/app/ktor-server.jar"]
