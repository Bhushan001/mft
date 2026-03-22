FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /build
COPY . .
RUN mvn -pl engine -am package -DskipTests -q

FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
RUN addgroup -S chrono && adduser -S chrono -G chrono
COPY --from=build /build/engine/target/*.jar app.jar
RUN find . -name "*original*" -delete
USER chrono
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
