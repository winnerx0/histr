FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
RUN apt-get update && apt-get install -y libstdc++6 && rm -rf /var/lib/apt/lists/*
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
