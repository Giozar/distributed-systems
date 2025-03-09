
#!/bin/bash

# Compilar el proyecto

mvn clean package

# Detener y eliminar el contenedor existente si existe

docker stop mi-tomcat || true

docker rm mi-tomcat || true

# Opción 1: Usar Dockerfile

echo "Construyendo imagen Docker..."

docker build -t mi-app-jsp .

# Crear la red si no existe
docker network create app-network

echo "Ejecutando contenedor..."

docker run -d \
  --name mi-tomcat \
  --network app-network \
  -p 8080:8080 \
  mi-app-jsp

# Opción 2: Usar imagen oficial (comentada)

# echo "Ejecutando contenedor con volumen..."

# docker run -d \

#   --name mi-tomcat \

#   --network app-network \

#   -p 8080:8080 \

#   -v $PWD/target/webapp-jsp-maven.war:/usr/local/tomcat/webapps/ROOT.war \

#   tomcat:9.0

echo "Aplicación desplegada. Accede a http://localhost:8080"

echo "Verificando logs:"

docker logs mi-tomcat

