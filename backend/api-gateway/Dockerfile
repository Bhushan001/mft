FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /build
COPY . .
RUN mvn -pl api-gateway -am package -DskipTests -q

FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
RUN addgroup -S chrono && adduser -S chrono -G chrono
COPY --from=build /build/api-gateway/target/*.jar app.jar
RUN find . -name "*original*" -delete
USER chrono
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
