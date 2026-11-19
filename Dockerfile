FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN sh ./mvnw -B package && cp target/chatapp-*.jar /tmp/chatapp.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /tmp/chatapp.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
