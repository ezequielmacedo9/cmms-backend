FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY . .


RUN if [ -f /app/pom.xml ]; then \
        cd /app && mvn clean package -DskipTests; \
    elif [ -f /app/cmms/pom.xml ]; then \
        cd /app/cmms && mvn clean package -DskipTests; \
    elif [ -f /app/cmms/_mvn/src/pom.xml ]; then \
        cd /app/cmms/_mvn/src && mvn clean package -DskipTests; \
    else echo "ERRO: pom.xml não encontrado"; exit 1; fi


FROM eclipse-temurin:17-jre

WORKDIR /app

# Copiar o JAR do build
COPY --from=build /app/cmms/_mvn/src/target/cmms-0.0.1-SNAPSHOT.jar ./cmms-0.0.1-SNAPSHOT.jar

EXPOSE 10000

CMD ["java", "-jar", "cmms-0.0.1-SNAPSHOT.jar"]