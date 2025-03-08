#!/bin/bash

# Compilar el proyecto
mvn clean package

# Detener y eliminar el contenedor existente si existe
docker stop mi-java-app || true
docker rm mi-java-app || true

# Construir la imagen
docker build -t mi-socket-app .

# Crear la red si no existe
docker network create app-network

# Ejecutar el contenedor
docker run -d \
  --name mi-java-app \
  --network app-network \
  -p 3000:3000 \
  mi-socket-app

echo "Aplicación desplegada. Verificando logs:"
docker logs mi-java-app
