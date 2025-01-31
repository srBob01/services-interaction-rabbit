FROM openjdk:21-jdk-slim-buster
LABEL authors="Nikita"

WORKDIR /app

# Копируем исходный код в контейнер
COPY . .

# Сборка проекта с использованием Gradle
RUN ./gradlew clean build

# Запуск приложения
CMD ["java", "-jar", "./build/libs/producer-0.0.1-SNAPSHOT.jar"]
