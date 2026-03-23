FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app

COPY . .

WORKDIR /app/cmms

RUN mvn clean package -DskipTests

EXPOSE 10000

CMD ["java", "-jar", "target/*.jar"]