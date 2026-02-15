# Use lightweight Java runtime
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy jar file
COPY target/*.jar app.jar

# Expose port (change if your app uses different port)
EXPOSE 8080

# Run application
ENTRYPOINT ["java","-jar","app.jar"]
