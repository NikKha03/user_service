 # Используем официальный OpenJDK образ для запуска приложения
FROM openjdk:21-jdk-slim

# Указываем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем jar файл из локальной машины в контейнер
COPY target/UserService-0.0.1-SNAPSHOT.jar UserService.jar

# Команда для запуска приложения в контейнере
ENTRYPOINT ["java", "-jar", "UserService.jar"]

# Открываем порт, на котором будет работать приложение
EXPOSE 8080
