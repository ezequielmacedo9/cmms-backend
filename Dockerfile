FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app

COPY . .


RUN echo "=== ARQUIVOS NA RAIZ ===" && ls -la
RUN echo "=== BUSCANDO pom.xml ===" && find /app -name "pom.xml" | head -10 || echo "NENHUM pom.xml"


RUN if [ -f /app/pom.xml ]; then echo "POM NA RAIZ"; cd /app && mvn clean package -DskipTests; \
    elif [ -f /app/cmms/pom.xml ]; then echo "POM EM /cmms"; cd /app/cmms && mvn clean package -DskipTests; \
    elif [ -f /app/cmms/_mvn/src/pom.xml ]; then echo "POM EM /cmms/_mvn/src"; cd /app/cmms/_mvn/src && mvn clean package -DskipTests; \
    else echo "ERRO: pom.xml não encontrado"; ls -la /app/; exit 1; fi

EXPOSE 10000
CMD ["java", "-jar", "target/cmms-0.0.1-SNAPSHOT.jar"]