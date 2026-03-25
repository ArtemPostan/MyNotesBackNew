# Stage 1: Build
# Используем зеркало mirror.gcr.io, чтобы избежать ошибки EOF
FROM mirror.gcr.io/library/maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Собираем JAR
RUN mvn clean package -DskipTests

# Stage 2: Run
# Здесь тоже используем зеркало
FROM mirror.gcr.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем результат сборки
COPY --from=build /app/target/*.jar app.jar

# Настройки для YDB (если используешь метаданные облака)
ENV USE_METADATA=true

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]