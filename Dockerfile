# ---------- Stage 1 : Build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copie d'abord le pom pour mettre en cache les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Puis le code source
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Stage 2 : Run ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Récupère le jar construit à l'étape précédente
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]