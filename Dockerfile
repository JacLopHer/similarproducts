FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY similarproducts-domain similarproducts-domain
COPY similarproducts-application similarproducts-application
COPY similarproducts-infrastructure similarproducts-infrastructure
COPY similarproducts-boot similarproducts-boot

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/similarproducts-boot/target/similarproducts-boot-*.jar app.jar

EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]

