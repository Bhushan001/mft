FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /build
COPY . .
RUN mvn -pl tenant-service -am package -DskipTests -q

FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
RUN addgroup -S chrono && adduser -S chrono -G chrono
COPY --from=build /build/tenant-service/target/*.jar app.jar
RUN find . -name "*original*" -delete
USER chrono
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
