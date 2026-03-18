FROM amazoncorretto:17-al2023-headless
WORKDIR /app
COPY build/libs/A3S-project-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]