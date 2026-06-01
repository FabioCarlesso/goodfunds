# syntax=docker/dockerfile:1.7

# --- Stage 1: build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .mvn/ .mvn/
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# --- Stage 2: runtime ---
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN groupadd --system goodfunds && useradd --system --gid goodfunds --home /app goodfunds

COPY --from=build /workspace/target/goodfunds-backend.jar /app/app.jar
# Cria o diretorio de uploads ainda na imagem para que, ao montar o volume nomeado
# pela primeira vez, o Docker herde o dono `goodfunds` (caso contrario o volume nasce
# como root e o processo nao-root falha com AccessDeniedException ao gravar a fatura).
RUN mkdir -p /app/uploads
RUN chown -R goodfunds:goodfunds /app
USER goodfunds

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
