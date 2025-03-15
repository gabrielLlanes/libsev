package io.sev.util.value;

public class LongWrapper {
    
    private long value;

    private LongWrapper() {
        value = 0L;
    }

    private LongWrapper(long value) {
        this.value = value;
    }

    public static LongWrapper of(long value) {
        return new LongWrapper(value);
    }

    public void increment() {
        value += 1L;
    }

    public void decrement() {
        value -= 1L;
    }

    public long value() {
        return value;
    }

    public void set(long value) {
        this.value = value;
    }

}
