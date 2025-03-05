package concurrence.server.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import concurrence.server.domain.Client;
import concurrence.server.domain.ClientRepository;

public class InMemoryClientRepository  implements ClientRepository{

    private final Map<Integer, Client> clients = new ConcurrentHashMap<>();
    @Override
    public void addClient(Client client) {
        clients.put(client.getId(), client);
    }

    @Override
    public void removeClient(int clientId) {
        clients.remove(clientId);
    }

    @Override
    public Client getClient(int clientId) {
        return clients.get(clientId);
    }

    @Override
    public List<Client> getAllClients() {
        return new ArrayList<>(clients.values());
    }

    @Override
    public int getClientCount() {
        return clients.size();
    }



}
