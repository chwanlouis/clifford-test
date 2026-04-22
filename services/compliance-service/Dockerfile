FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY ../../pom.xml /parent-pom.xml
COPY src ./src
RUN apk add --no-cache maven && mvn -f pom.xml package -DskipTests -o || mvn -f pom.xml package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S itms && adduser -S itms -G itms
COPY --from=builder /app/target/*.jar app.jar
USER itms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
