package io.sev.socket;

import java.io.IOException;
import java.net.ConnectException;

public class Errors {

    public static final int ERRNO_ECONNREFUSED = 111;
    
    public static final int ERRNO_ECONNRESET = 104;

    public static final int ERRNO_EAGAIN = 11;

    public static final int ERRNO_EWOULDBLOCK = ERRNO_EAGAIN;

    public static void ioException(int err) throws IOException {
        switch(err) {
            case ERRNO_ECONNREFUSED:
                throw new ConnectException("Connection refused");
            case ERRNO_ECONNRESET:
                throw new ConnectException("Connection reset");
            default:
                throw new IOException("IOException: " + err);
        }
    }
}
