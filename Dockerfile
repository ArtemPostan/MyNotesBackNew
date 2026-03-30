# Stage 1: Build
FROM mirror.gcr.io/library/maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Собираем JAR
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM mirror.gcr.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Настройки для YDB
ENV USE_METADATA=true

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]