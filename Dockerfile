FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY familia-connect/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY familia-connect/src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
COPY carteirinhas/example/carteirinha-example.pdf /app/carteirinhas/example/carteirinha-example.pdf
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
