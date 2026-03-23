FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app/cmms
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/cmms/target/cmms-0.0.1-SNAPSHOT.jar cmms.jar
EXPOSE 10000
CMD ["java", "-jar", "cmms.jar"]