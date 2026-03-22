FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /build
COPY . .
RUN mvn -pl service-registry -am package -DskipTests -q

FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
RUN addgroup -S chrono && adduser -S chrono -G chrono
COPY --from=build /build/service-registry/target/*.jar app.jar
RUN find . -name "*original*" -delete
USER chrono
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
