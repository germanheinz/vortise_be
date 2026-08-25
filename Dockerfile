FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Cachear dependencias cuando solo cambia el pom
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn -B -DskipTests package --no-transfer-progress

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Copia el JAR compilado desde la etapa build
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
