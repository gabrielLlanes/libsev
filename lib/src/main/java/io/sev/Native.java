package io.sev;

public class Native {

    //load library once
    static {
        System.loadLibrary("sev_uring");
    }

}
