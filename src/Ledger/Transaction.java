package Ledger;

import java.util.*;

public class Transaction {
    long128 id;
    List<UTxO> inputs;
    List<Transfer> outputs;

    public Transaction(long128 id, ArrayList<UTxO> inputs, ArrayList<Transfer> outputs){
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public long getTransferValue(long128 address){
        for (Transfer t : outputs){
            if (t.address.equals(address)){
                return t.coins;
            }
        }
        throw new NoSuchElementException("Transfer not found");
    }

    public long getTotalValue(){
        long v = 0;
        for (Transfer t : outputs){
            v += t.coins;
        }
        return v;
    }

    public long128 getInputAddress(){
        return inputs.get(0).address;
    }
}
