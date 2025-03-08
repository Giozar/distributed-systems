package concurrence;

import concurrence.server.application.ServerService;
import concurrence.server.domain.ClientRepository;
import concurrence.server.infrastructure.InMemoryClientRepository;
import concurrence.shared.Logger;

public class Main {
    public static void main(String[] args) {

        Logger logger = new Logger();
        ClientRepository repository = new InMemoryClientRepository();
        int port = 3000;


        ServerService serverService = ServerService.getInstance(port, repository, logger);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if(ServerService.getInstance() != null && ServerService.getInstance().isRunning()) {
                    ServerService.getInstance().stopt();
                }
            }));

            try {
                logger.info("Iniciando servidor...");
                serverService.start();

                logger.info("Servidor iniciado. Presiona Ctrl+C para detenerlo...");

                ServerService sameServerService = ServerService.getInstance();
                logger.info("Clientes conectados actualmente: " + sameServerService.getClientCount());
            } catch (Exception e) {
                logger.error("Error al iniciar el servidor", e);
                serverService.stopt();
            }
        System.out.println("Servidor corriendo en puerto " + port);
        
    }
}