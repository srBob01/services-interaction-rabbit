FROM openjdk:21-jdk-slim-buster
LABEL authors="Nikita"

WORKDIR /app

COPY . .

RUN ./gradlew clean build

CMD ["java", "-jar", "./build/libs/item-consumer-0.0.1-SNAPSHOT.jar"]
