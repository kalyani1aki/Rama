# Build the JAR first: ./mvnw package -DskipTests
# Then build the Docker image: docker build -t rama-backend .
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
