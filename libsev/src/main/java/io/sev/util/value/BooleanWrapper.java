package io.sev.util.value;

public class BooleanWrapper {
    
    private boolean value;

    private BooleanWrapper() {
        value = false;
    }

    private BooleanWrapper(boolean value) {
        this.value = value;
    }

    public static BooleanWrapper of(boolean value) {
        return new BooleanWrapper(value);
    }

    public boolean value() {
        return value;
    }

    public void set(boolean value) {
        this.value = value;
    }

}
