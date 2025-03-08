# Servidor Java Concurrente

Un servidor Java que implementa una arquitectura híbrida para manejar múltiples clientes de forma concurrente. El proyecto sigue los principios SOLID y patrones de diseño como Singleton, Repository e Dependency Injection para garantizar una estructura limpia y mantenible.

## Características

- **Gestión concurrente de clientes**: Manejo de múltiples clientes simultáneamente utilizando un pool de hilos.
- **Arquitectura híbrida**: Combinación de Clean Architecture, Arquitectura Hexagonal y Screaming Architecture.
- **Implementación de Singleton**: Garantiza una única instancia del servidor en toda la aplicación.
- **Implementación de Repository**: Aísla la lógica de acceso a datos, independientemente de su origen..
- **Implementación de Dependency Injection**: Gestiona y desacopla las dependencias para mejorar la mantenibilidad..
- **Conteo de clientes conectados**: Seguimiento en tiempo real de los clientes conectados.
- **Comunicación bidireccional**: Los clientes pueden enviar mensajes al servidor y recibir respuestas.

## Estructura del Proyecto

```
📦 src/main/java/com/miempresa
 ┣ 📂 server                           // Funcionalidad principal (Screaming Architecture)
 ┃ ┣ 📂 domain                         // Capa interna (Clean Architecture)
 ┃ ┃ ┣ 📜 Client.java                  // Entidad de dominio
 ┃ ┃ ┣ 📜 ClientRepository.java        // Puerto (Arquitectura Hexagonal)
 ┃ ┃ ┗ 📜 ServerExceptions.java        // Excepciones de dominio
 ┃ ┣ 📂 application                    // Capa de casos de uso (Clean Architecture)
 ┃ ┃ ┗ 📜 ServerService.java           // Servicio principal
 ┃ ┣ 📂 infrastructure                 // Capa externa (Clean Architecture)
 ┃ ┃ ┗ 📜 InMemoryClientRepository.java // Adaptador (Arquitectura Hexagonal)
 ┃ ┗ 📜 index.java                     // Punto de entrada para el módulo server
 ┣ 📂 shared                           // Componentes compartidos
 ┃ ┗ 📜 Logger.java                    // Utilidad para logging
 ┗ 📜 MainApplication.java             // Clase principal con método main
```

## Arquitectura Implementada

Este proyecto implementa una **arquitectura híbrida** que combina elementos de:

### Arquitectura Hexagonal (Puertos y Adaptadores)
- **Puertos**: Interfaces como `ClientRepository` que definen cómo el dominio interactúa con el exterior
- **Adaptadores**: Implementaciones como `InMemoryClientRepository` que cumplen con esas interfaces

### Clean Architecture
- **Capas organizadas**: Domain → Application → Infrastructure
- **Regla de dependencia**: Las capas externas dependen de las internas, nunca al revés
- **Inversión de control**: Las dependencias se inyectan de fuera hacia dentro

### Screaming Architecture
- **Organización por funcionalidad**: La estructura "grita" el propósito (server)
- **Casos de uso explícitos**: El `ServerService` implementa el caso de uso principal

## Principios SOLID Implementados

1. **Principio de Responsabilidad Única (SRP)**
   - Cada clase tiene una única responsabilidad
   - Ejemplos: `Client` (entidad), `ServerService` (lógica), `InMemoryClientRepository` (almacenamiento)

2. **Principio de Abierto/Cerrado (OCP)**
   - Extensible sin modificar el código existente
   - Ejemplo: Se pueden añadir nuevas implementaciones de `ClientRepository` sin modificar `ServerService`

3. **Principio de Sustitución de Liskov (LSP)**
   - Las implementaciones de las interfaces pueden ser sustituidas
   - Ejemplo: `ServerService` trabaja con cualquier implementación de `ClientRepository`

4. **Principio de Segregación de Interfaces (ISP)**
   - Interfaces específicas en lugar de una grande
   - Ejemplo: `ClientRepository` solo contiene los métodos necesarios

5. **Principio de Inversión de Dependencias (DIP)**
   - Dependencia de abstracciones, no implementaciones
   - Ejemplo: `ServerService` depende de `ClientRepository` (interfaz), no de `InMemoryClientRepository`

## Patrones de Diseño Implementados

### Patrón Repositorio

Implementado a través de la interfaz `ClientRepository` y su implementación `InMemoryClientRepository`:

```java
// Definición del repositorio
public interface ClientRepository {
    void addClient(Client client);
    void removeClient(int clientId);
    Client getClient(int clientId);
    List<Client> getAllClients();
    int getClientCount();
}

// Uso del repositorio
clientRepository.addClient(client);
int totalClients = clientRepository.getClientCount();
```

Este patrón desacopla la lógica de negocio de la lógica de acceso a datos, permitiendo:
- Centralizar la lógica de persistencia
- Facilitar el cambio entre diferentes implementaciones (memoria, base de datos, etc.)
- Simplificar las pruebas mediante mocks o implementaciones en memoria

### Patrón Singleton

Implementado en `ServerService` para garantizar una única instancia del servidor:

```java
// Obtener instancia
ServerService serverService = ServerService.getInstance(port, clientRepository, logger);

// En cualquier otra parte de la aplicación
ServerService server = ServerService.getInstance();
```

### Inyección de Dependencias

```java
// Las dependencias se inyectan en el constructor
private ServerService(int port, ClientRepository clientRepository, Logger logger) {
    this.port = port;
    this.clientRepository = clientRepository;
    this.logger = logger;
    // ...
}
```

## Cómo Ejecutar

1. Compile el proyecto usando Maven o su IDE preferido
2. Ejecute la clase `Main`
3. Conéctese al servidor usando un cliente TCP en el puerto 8080


## Ejecutar aplicación usando Docker
```bash

# Asegúrate de estar en la raíz del proyecto
cd ~/process-thread

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

```
## Ejecutando un script
```bash

   # Ejecuta el siguiente comando
   ./build-and-run.sh
```

## Futuras Mejoras

- Implementación de protocolos de comunicación
- Persistencia de datos de clientes
- Panel de administración
- Métricas y monitoreo