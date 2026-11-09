# Stage 1: build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy only what's needed to resolve dependencies first (cache-friendly)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

# Now copy source and build
COPY src src
RUN ./mvnw -q package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

