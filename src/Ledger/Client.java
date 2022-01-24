package Ledger;
import java.util.ArrayList;
import java.util.List;

public class Client {
    long128 address;
    ArrayList<Server> servers;

    public Client(long128 address, ArrayList<Server> servers){
        this.address = address;
        this.servers = servers;
    }

    public Server getServerResponsibleFor(long128 address){
        int num_servers = servers.size();
        int server_index = address.modulo(num_servers);
        return servers.get(server_index);
    }

    public boolean sendCoins(long128 address, long amount){
        Server server = getServerResponsibleFor(this.address);
        Transfer transfer = new Transfer(address, amount);
        ArrayList<Transfer> transfers = new ArrayList<>();
        transfers.add(transfer);
        return server.sendCoins(this.address, transfers);
    }

    public boolean submitTransaction(Transaction transaction){
        long128 address = transaction.getInputAddress();
        Server server = getServerResponsibleFor(address);
        return server.submitTransaction(transaction);
    }

    public boolean submitTransactionList(List<Transaction> transactions){
        return false;
    }

    public List<UTxO> getUTxO(long128 address){
        Server server = getServerResponsibleFor(address);
        return server.getUTxOs(address);
    }

    public List<Transaction> getTransactionHistoryFor(long128 address, long max){
        Server server = getServerResponsibleFor(address);
        return server.getTransactions(address, max);
    }

    public List<Transaction> getTransactionHistory(){
        ArrayList<Transaction> transactions = new ArrayList<>();
        for (Server server : servers){
            ArrayList<Transaction> transactions_server =  server.getTransactions(0);
            transactions.addAll(transactions_server);
        }
        return transactions;
    }
}
