# Etapa 1: compilar el proyecto con Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa 2: imagen liviana solo con el JAR
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=America/Lima
COPY --from=build /app/target/*.jar app.jar
# Memoria ajustada al plan gratuito de Render (512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -Duser.timezone=America/Lima"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
