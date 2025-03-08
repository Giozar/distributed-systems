FROM openjdk:17-slim

WORKDIR /app

# Copiar el WAR desde el directorio target
COPY target/process-thread-1.0-SNAPSHOT.war /app/

# Extraer el WAR
RUN mkdir -p /app/extracted && \
    cd /app/extracted && \
    jar -xf /app/process-thread-1.0-SNAPSHOT.war

# Exponer el puerto 3000
EXPOSE 3000

# Establecer el punto de entrada para ejecutar tu aplicación
ENTRYPOINT ["java", "-cp", "/app/extracted:/app/extracted/WEB-INF/classes:/app/extracted/WEB-INF/lib/*", "concurrence.Main"]