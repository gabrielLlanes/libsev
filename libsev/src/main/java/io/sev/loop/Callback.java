package io.sev.loop;

public interface Callback<L extends Loop<L, T>, T extends Completion<L, T>> {

    public abstract boolean invoke(Object context, L loop, T completion, int result);
    
}
