package io.sev.loop.uring;

import io.sev.loop.Callback;

public interface UringCallback extends Callback<UringLoop, UringCompletion> {

    @Override
    public boolean invoke(Object context, UringCompletion completion, int result);
    
}
