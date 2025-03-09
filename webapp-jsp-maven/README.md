# Aplicación JSP con Maven y Docker

Este proyecto es una aplicación web JSP básica creada con Maven (maven-archetype-webapp) y desplegada en Tomcat utilizando Docker.

## Requisitos previos

- Java JDK 8 o superior
- Maven 3.6 o superior
- Docker

## Estructura del proyecto

```
webapp-jsp-maven/
├── src/
│   └── main/
│       └── webapp/      (contenido web)
│           ├── WEB-INF/
│           │   └── web.xml
│           └── index.jsp
├── target/              (generado por Maven)
├── .gitignore
├── Dockerfile
├── deploy-webapp.sh
├── pom.xml
└── README.md
```

## Construcción del proyecto

Para construir el proyecto y generar el archivo WAR:

```bash
mvn clean package
```

## Despliegue con Docker

### Opción 1: Usando Dockerfile

1. Se crea un archivo `Dockerfile` en la raíz del proyecto con el siguiente contenido:

```dockerfile
FROM tomcat:9.0-jdk8

# Eliminar las aplicaciones de ejemplo
RUN rm -rf /usr/local/tomcat/webapps/*

# Copiar tu archivo WAR a la carpeta webapps
COPY target/webapp-jsp-maven.war /usr/local/tomcat/webapps/ROOT.war

# Exponer el puerto 8080
EXPOSE 8080

# Comando para iniciar Tomcat
CMD ["catalina.sh", "run"]
```

2. Construye la imagen Docker:

```bash
docker build -t mi-app-jsp .
```

3. Ejecuta el contenedor:

```bash
docker run -it --rm -p 8080:8080 mi-app-jsp
```

### Opción 2: Usando volúmenes con la imagen oficial de Tomcat

Esta opción te permite actualizar la aplicación sin reconstruir la imagen:

```bash
# Desde el directorio raíz del proyecto
docker run -d \
  --name mi-tomcat \
  -p 8080:8080 \
  -v $PWD/target/webapp-jsp-maven.war:/usr/local/tomcat/webapps/ROOT.war \
  tomcat:9.0
```

Si estás en otro directorio:

```bash
# Desde otro directorio
docker run -d \
  --name mi-tomcat \
  -p 8080:8080 \
  -v /ruta/completa/a/webapp-jsp-maven.war:/usr/local/tomcat/webapps/ROOT.war \
  tomcat:9.0
```

## Script de automatización

Puedes crear un script para automatizar todo el proceso:

```bash
#!/bin/bash
# Compilar el proyecto
mvn clean package

# Detener y eliminar el contenedor existente si existe
docker stop mi-tomcat || true
docker rm mi-tomcat || true

# Opción 1: Usar Dockerfile
echo "Construyendo imagen Docker..."
docker build -t mi-app-jsp .

echo "Ejecutando contenedor..."
docker run -d \
  --name mi-tomcat \
  -p 8080:8080 \
  mi-app-jsp

# Opción 2: Usar imagen oficial (comentada)
# echo "Ejecutando contenedor con volumen..."
# docker run -d \
#   --name mi-tomcat \
#   -p 8080:8080 \
#   -v $PWD/target/webapp-jsp-maven.war:/usr/local/tomcat/webapps/ROOT.war \
#   tomcat:9.0

echo "Aplicación desplegada. Accede a http://localhost:8080"
echo "Verificando logs:"
docker logs mi-tomcat
```

Guarda este contenido en un archivo llamado `deploy-webapp.sh` y dale permisos de ejecución:

```bash
chmod +x deploy-webapp.sh
```

Para ejecutarlo:

```bash
./deploy-webapp.sh
```

## Acceso a la aplicación

Una vez desplegada, la aplicación estará disponible en:
```
http://localhost:8080
```

## Solución de problemas

- **La aplicación no carga:** Verifica los logs del contenedor con `docker logs mi-tomcat`
- **Puerto ocupado:** Si el puerto 8080 está ocupado, cambia el mapeo de puertos (ej: `-p 8888:8080`)
- **Cambios no visibles:** Si usas la opción 1 (Dockerfile), necesitas reconstruir la imagen para ver los cambios

## Información adicional

- La aplicación se despliega como ROOT.war para que sea accesible en la raíz del servidor (`/`)
- Para aplicaciones en producción, considera configurar ajustes de seguridad adicionales