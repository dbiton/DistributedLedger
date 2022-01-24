package Ledger;

public class UTxO {
    long128 transaction_id;
    long128 address;
    long coins;

    public UTxO(long128 transaction_id, long128 address, long coins){
        this.transaction_id = transaction_id;
        this.address = address;
        this.coins = coins;
    }
}
