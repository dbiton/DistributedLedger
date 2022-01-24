package Ledger;

public class Transfer {
    long128 address;
    long coins;

    public Transfer(long128 address, long coins){
        this.address = address;
        this.coins = coins;
    }
}
