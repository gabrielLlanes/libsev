package io.sev.util.errors;

public class UnixException extends Exception {

    public static final int EPERM = 1;
    public static final int ENOENT = 2;
    public static final int EINTR = 4;
    public static final int EIO = 5;
    public static final int ENXIO = 6;
    public static final int EBADF = 9;
    public static final int EAGAIN = 11;
    public static final int EWOULDBLOCK = EAGAIN;
    public static final int ENOMEM = 12;
    public static final int EACCES = 13;
    public static final int EFAULT = 14;
    public static final int EBUSY = 16;
    public static final int EISDIR = 21;
    public static final int EINVAL = 22;
    public static final int ENFILE = 23;
    public static final int EMFILE = 24;
    public static final int EFBIG = 27;
    public static final int ENOSPC = 28;
    public static final int EPIPE = 32;
    public static final int ETIME = 62;
    public static final int EPROTO = 71;
    public static final int EBADFD = 77;
    public static final int ENOTSOCK = 88;
    public static final int EDESTADDRREQ = 89;
    public static final int EMSGSIZE = 90;
    public static final int EPROTOTYPE = 91;
    public static final int EOPNOTSUPP = 95;
    public static final int EAFNOSUPPORT = 97;
    public static final int EADDRINUSE = 98;
    public static final int EADDRNOTAVAIL = 99;
    public static final int ENETUNREACH = 101;
    public static final int ECONNABORTED = 103;
    public static final int ECONNRESET = 104;
    public static final int ENOBUFS = 105;
    public static final int EISCONN = 106;
    public static final int ENOTCONN = 107;
    public static final int ETIMEDOUT = 110;
    public static final int ECONNREFUSED = 111;
    public static final int EALREADY = 114;
    public static final int EINPROGRESS = 115;
    public static final int EDQUOT = 122;
    public static final int ECANCELED = 125;

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
