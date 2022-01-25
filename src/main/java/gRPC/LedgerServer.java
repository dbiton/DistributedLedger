package gRPC;

import Ledger.Transaction;
import Ledger.UTxO;
import Ledger.Transfer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.math.BigInteger;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;


public class LedgerServer {
    long id;

    BigInteger last_tid;

    List<LedgerServer> ledgerServers;

    List<Transaction> transactions;
    List<Long> timestamps;
    List<UTxO> UTxOs;

    public LedgerServer(long id) {
        this.transactions = new ArrayList<>();
        this.timestamps = new ArrayList<>();
        this.UTxOs = new ArrayList<>();
        this.id = id;
        this.last_tid = BigInteger.valueOf(id);
        // if responsible for genesis block
        if (id == 0) {
            UTxO utxo = new UTxO(BigInteger.ZERO, BigInteger.ZERO, Long.MAX_VALUE);
            UTxOs.add(utxo);
        }
    }

    public void setServers(List<LedgerServer> ledgerServers) {
        this.ledgerServers = ledgerServers;
    }

    public LedgerServer getServerResponsibleFor(BigInteger address) {
        int num_servers = ledgerServers.size();
        int server_index = address.remainder(BigInteger.valueOf(num_servers)).intValue();
        return ledgerServers.get(server_index);
    }

    public BigInteger genTransactionID() {
        BigInteger tid = last_tid;
        this.last_tid = this.last_tid.add(BigInteger.valueOf(ledgerServers.size()));
        return tid;
    }

    public boolean sendCoins(BigInteger address, List<Transfer> outputs) {
        // calculate total needed coins
        long total_value = 0;
        for (Transfer t : outputs) {
            total_value += t.getCoins();
        }

        // consume UTxOs for payment
        ArrayList<UTxO> inputs = consumeUTxOs(address, total_value);
        // not enough money
        if (inputs.isEmpty()) {
            return false;
        }

        BigInteger transaction_id = genTransactionID();

        for (Transfer transfer : outputs) {
            LedgerServer ledgerServer = getServerResponsibleFor(transfer.getAddress());
            ledgerServer.receiveTransfer(transfer, transaction_id);
        }

        // pay self reminder
        long reminder = -total_value;
        for (UTxO u : inputs){
            reminder += u.getCoins();
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
    public ArrayList<UTxO> consumeUTxOs(BigInteger address, long coins) {
        long coins_curr = 0;
        ArrayList<UTxO> us = new ArrayList<>();
        for (UTxO u : this.UTxOs) {
            if (u.getAddress().equals(address)) {
                us.add(u);
                coins_curr += u.getCoins();
                if (coins_curr >= coins) {
                    break;
                }
            }
        }
        // not enough coins to consume
        if (coins_curr < coins) {
            return new ArrayList<>();
        }
        for (UTxO u : us) {
            this.UTxOs.remove(u);
        }
        return us;
    }

    public void receiveTransfer(Transfer transfer, BigInteger transaction_id) {
        UTxO u = new UTxO(transaction_id, transfer.getAddress(), transfer.getCoins());
        UTxOs.add(u);
    }

    public ArrayList<UTxO> getUTxOs(BigInteger address) {
        ArrayList<UTxO> us = new ArrayList<>();
        for (UTxO u : this.UTxOs) {
            if (u.getAddress().equals(address)) {
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

    public ArrayList<Transaction> getTransactions(BigInteger address, long max) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        long counter = 0;
        for (Transaction transaction : this.transactions) {
            if (transaction.getInputAddress().equals(address)) {
                transactions.add(transaction);
                counter += 1;
                if (max > 0 && counter == max) {
                    break;
                }
            }
        }
        return transactions;
    }

    // used when coins value is unknown
    public long getCoinsUTxO(UTxO u){
        LedgerServer ledgerServer = getServerResponsibleFor(u.getAddress());
        return ledgerServer.getCoinsTransfer(u.getTransaction_id(), u.getAddress());
    }

    public long getCoinsTransfer(BigInteger transaction_id, BigInteger address){
        for (Transaction transaction : transactions){
            if (transaction.getId().equals(transaction_id)){
                for (Transfer transfer : transaction.getOutputs()){
                    if (transfer.getAddress().equals(address)){
                        return transfer.getCoins();
                    }
                }
                return 0;
            }
        }
        return 0;
    }

    public boolean submitTransaction(Transaction transaction){
        long total_value = 0;
        BigInteger address = transaction.getInputAddress();
        for (UTxO u : transaction.getInputs()){
            // only 1 address per transaction allowed
            if (!u.getAddress().equals(address)){
                return false;
            }
            // UTxO must be present
            if (!this.UTxOs.contains(u)){
                return false;
            }
            // now, we need to manually find out coins for each UTxO (since we don't get that)
            u.setCoins(getCoinsUTxO(u));
            total_value += u.getCoins();
        }
        for (Transfer transfer : transaction.getOutputs()){
            total_value -= transfer.getCoins();
        }
        // outputs don't match inputs
        if (total_value != 0){
            return false;
        }

        // consume UTxOs
        for (UTxO u : transaction.getInputs()){
            this.UTxOs.remove(u);
        }

        BigInteger transaction_id = genTransactionID();

        // transfer coins
        transaction.setId(transaction_id);
        for (Transfer transfer : transaction.getOutputs()) {
            LedgerServer ledgerServer = getServerResponsibleFor(transfer.getAddress());
            ledgerServer.receiveTransfer(transfer, transaction_id);
        }

        // add transaction
        transactions.add(transaction);
        return true;
    }
}