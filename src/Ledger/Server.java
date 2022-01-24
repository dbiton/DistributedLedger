package Ledger;

import java.util.ArrayList;

public class Server {
    long id;
    long128 last_tid;

    ArrayList<Server> servers;

    ArrayList<Transaction> transactions;
    ArrayList<Long> timestamps;
    ArrayList<UTxO> UTxOs;

    public Server(long id) {
        this.transactions = new ArrayList<>();
        this.timestamps = new ArrayList<>();
        this.UTxOs = new ArrayList<>();
        this.id = id;
        this.last_tid = new long128(id);
        // if responsible for genesis block
        if (id == 0) {
            UTxO utxo = new UTxO(new long128(0), new long128(0), Long.MAX_VALUE);
            UTxOs.add(utxo);
        }
    }

    public void setServers(ArrayList<Server> servers) {
        this.servers = servers;
    }

    public Server getServerResponsibleFor(long128 address) {
        int num_servers = servers.size();
        int server_index = address.modulo(num_servers);
        return servers.get(server_index);
    }

    public long128 genTransactionID() {
        long128 tid = last_tid;
        last_tid = last_tid.add(new long128(servers.size()));
        return tid;
    }

    public boolean sendCoins(long128 address, ArrayList<Transfer> outputs) {
        // calculate total needed coins
        long total_value = 0;
        for (Transfer t : outputs) {
            total_value += t.coins;
        }

        // consume UTxOs for payment
        ArrayList<UTxO> inputs = consumeUTxOs(address, total_value);
        // not enough money
        if (inputs.isEmpty()) {
            return false;
        }

        long128 transaction_id = genTransactionID();

        for (Transfer transfer : outputs) {
            Server server = getServerResponsibleFor(transfer.address);
            server.receiveTransfer(transfer, transaction_id);
        }

        // pay self reminder
        long reminder = -total_value;
        for (UTxO u : inputs){
            reminder += u.coins;
        }
        if (reminder > 0) {
            Transfer transfer = new Transfer(address, reminder);
            outputs.add(transfer);
            receiveTransfer(transfer, transaction_id);
        }

        Transaction transaction = new Transaction(transaction_id, inputs, outputs);
        transactions.add(transaction);
        return true;
    }

    // returns UTxOs with total values above coins, or an empty list if we don't have enough
    public ArrayList<UTxO> consumeUTxOs(long128 address, long coins) {
        long coins_curr = 0;
        ArrayList<UTxO> us = new ArrayList<>();
        for (UTxO u : this.UTxOs) {
            if (u.address.equals(address)) {
                us.add(u);
                coins_curr += u.coins;
                if (coins_curr >= coins) {
                    break;
                }
            }
        }
        // not enough coins to consume
        if (coins_curr < coins) {
            return new ArrayList<UTxO>();
        }
        for (UTxO u : us) {
            this.UTxOs.remove(u);
        }
        return us;
    }

    public void receiveTransfer(Transfer transfer, long128 transaction_id) {
        UTxO u = new UTxO(transaction_id, transfer.address, transfer.coins);
        UTxOs.add(u);
    }

    public ArrayList<UTxO> getUTxOs(long128 address) {
        ArrayList<UTxO> us = new ArrayList<>();
        for (UTxO u : this.UTxOs) {
            if (u.address == address) {
                us.add(u);
            }
        }
        return us;
    }

    public ArrayList<Transaction> getTransactions(long max) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        long counter = 0;
        for (Transaction transaction : this.transactions) {
            transactions.add(transaction);
            counter += 1;
            if (max > 0 && counter == max) {
                break;
            }
        }
        return transactions;
    }

    public ArrayList<Transaction> getTransactions(long128 address, long max) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        long counter = 0;
        for (Transaction transaction : this.transactions) {
            if (transaction.getInputAddress() == address) {
                transactions.add(transaction);
                counter += 1;
                if (max > 0 && counter == max) {
                    break;
                }
            }
        }
        return transactions;
    }
}
