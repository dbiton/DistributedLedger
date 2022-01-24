package Ledger;

public class long128 {
    long low;
    long high;

    public long128(long low, long high){
        this.low = low;
        this.high = high;
    }

    public long128(long low){
        this.low = low;
        this.high = 0;
    }

    public int modulo(int n){
        return (int) (this.low % n);
    }

    public boolean equals(long128 o){
        return o.low == this.low && o.high == this.high;
    }

    public long128 add(long128 o){
        return new long128(low + o.low);
    }
}
