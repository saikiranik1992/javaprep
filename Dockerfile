# Stage 1: Build Spring Boot + Thymeleaf Application with JDK 21
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY pom.xml .
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production Runtime with JDK 21 (needed for live javac/java IDE sandbox execution)
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
