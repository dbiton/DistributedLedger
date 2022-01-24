package Ledger;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        int num_servers = 8;
        ArrayList<Server> servers = new ArrayList<>();
        for (int i=0; i<num_servers; i++) {
            Server server = new Server(i);
            servers.add(server);
        }
        for (Server server : servers){
            server.setServers(servers);
        }

        Client client = new Client(new long128(0), servers);
        client.sendCoins(new long128(32), 16);
    }
}
