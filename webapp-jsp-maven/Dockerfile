FROM tomcat:9.0-jdk8

# Eliminar las aplicaciones de ejemplo
RUN rm -rf /usr/local/tomcat/webapps/*

# Copiar tu archivo WAR a la carpeta webapps
COPY target/webapp-jsp-maven.war /usr/local/tomcat/webapps/ROOT.war

# Exponer el puerto 8080
EXPOSE 8080

# Comando para iniciar Tomcat
CMD ["catalina.sh", "run"]