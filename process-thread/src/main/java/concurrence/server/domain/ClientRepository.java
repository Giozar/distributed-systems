package concurrence.server.domain;

import java.util.List;

public interface ClientRepository {

    void addClient(Client client);
    void removeClient(int clientId);
    Client getClient(int clientId);
    List<Client> getAllClients();
    int getClientCount();

}
