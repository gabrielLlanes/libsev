package io.sev.util.unix;

public class UnixException extends Exception {

    private final int errno;

    private UnixException(int errnoNeg) {
        //TODO: make descriptive error messages corresponding to errno
        super("Unix Error: " + -errnoNeg);
        this.errno = -errnoNeg;
    }

    public int errno() {
        return errno;
    }

    public static void unixException(int errnoNeg) throws UnixException {
        throw new UnixException(errnoNeg);
    }
}
