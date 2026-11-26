# ---------- Build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependências primeiro: a camada fica em cache enquanto o pom.xml não mudar
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests package && cp target/compras-cotacoes-*.jar app.jar

# ---------- Runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# apk upgrade aplica os patches do Alpine publicados depois da imagem base (ex.: libexpat, zlib)
RUN apk upgrade --no-cache && addgroup -S app && adduser -S app -G app
COPY --from=build /app/app.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health" | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
