package concurrence.server;

import concurrence.server.application.ServerService;
import concurrence.server.domain.ClientRepository;
import concurrence.server.infrastructure.InMemoryClientRepository;
import concurrence.shared.Logger;

public class index {

    private index(){}

    public static ServerService startServer( int port ){
        Logger logger = new Logger();
        ClientRepository repository = new InMemoryClientRepository();

        ServerService server = ServerService.getInstance(port, repository, logger);
        server.start();
        return server;

    }
}
