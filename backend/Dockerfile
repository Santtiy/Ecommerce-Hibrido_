# Dockerfile UNICO y parametrizado para los siete microservicios.
# Contexto de build: la raiz del repo (necesita backend/ y frontend/).

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY backend/pom.xml backend/
COPY backend/common/pom.xml backend/common/
COPY backend/order-api/pom.xml backend/order-api/
COPY backend/analytics-service/pom.xml backend/analytics-service/
COPY backend/order-processor/pom.xml backend/order-processor/
COPY backend/billing-worker/pom.xml backend/billing-worker/
COPY backend/inventory-worker/pom.xml backend/inventory-worker/
COPY backend/notification-worker/pom.xml backend/notification-worker/
COPY backend/dashboard-aggregator/pom.xml backend/dashboard-aggregator/

WORKDIR /build/backend
RUN mvn -B -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline \
        -DexcludeGroupIds=co.edu.proyecto

WORKDIR /build
COPY backend/  backend/
COPY frontend/ frontend/
WORKDIR /build/backend
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

ARG MODULO

RUN addgroup -S ecommerce && adduser -S -G ecommerce -H -s /sbin/nologin ecommerce

WORKDIR /app

COPY --from=builder --chown=ecommerce:ecommerce /build/backend/${MODULO}/target/${MODULO}-1.0.0.jar app.jar

RUN mkdir -p /app/datos && chown ecommerce:ecommerce /app/datos

USER ecommerce

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
