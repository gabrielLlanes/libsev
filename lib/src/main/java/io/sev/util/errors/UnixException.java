package io.sev.util.errors;

public class UnixException extends Exception {

    public static final int EINTR = 4;
    public static final int ENXIO = 6;
    public static final int EBADF = 9;
    public static final int EAGAIN = 11;
    public static final int EWOULDBLOCK = EAGAIN;
    public static final int ENOMEM = 12;
    public static final int EFAULT = 14;
    public static final int EBUSY = 16;
    public static final int EINVAL = 22;
    public static final int ETIME = 62;
    public static final int EBADFD = 77;
    public static final int EOPNOTSUPP = 95;
    public static final int EADDRINUSE = 98;
    public static final int EADDRNOTAVAIL = 99;
    public static final int ECONNRESET = 104;
    public static final int EISCONN = 106;
    public static final int ENOTCONN = 107;
    public static final int ECONNREFUSED = 111;
    public static final int EINPROGRESS = 115;

    private final int errno;

    private UnixException(int errnoNeg) {
        super("Unix Error: " + -errnoNeg);
        this.errno = -errnoNeg;
    }

    public int errno() {
        return errno;
    }

    public static void unixException(int errno) throws UnixException {
        throw new UnixException(errno);
    }
}
