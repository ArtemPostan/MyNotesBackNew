# Stage 1: Build (оставляем без изменений)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# COPY --from=build /app/src/main/resources/authorized_key.json /app/authorized_key.json

# ENV SA_KEY_PATH=/app/authorized_key.json
ENV USE_METADATA=true

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]