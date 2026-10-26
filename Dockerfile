# Stage 1: Build Spring Boot + Thymeleaf Application with JDK 21
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY pom.xml .
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production Runtime with JDK 21 (provides in-memory javax.tools.JavaCompiler for instant IDE execution)
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:MaxRAMPercentage=70.0 -XX:TieredStopAtLevel=1"
ENTRYPOINT ["java", "-jar", "app.jar"]
