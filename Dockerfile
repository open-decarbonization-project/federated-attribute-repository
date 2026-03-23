FROM eclipse-temurin:21-jre
RUN groupadd -r repository && useradd -r -g repository -d /app repository
COPY repository-server/target/quarkus-app /app
RUN mkdir -p /app/data/keys && chown -R repository:repository /app
WORKDIR /app
USER repository
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
