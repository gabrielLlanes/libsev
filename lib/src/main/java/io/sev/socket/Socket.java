package io.sev.socket;

import java.io.IOException;
import java.net.Inet4Address;

import io.sev.Native;

public class Socket {

    public static final int SOL_SOCKET = 1;

    public static final int IPPROTO_TCP = 6;

    public static final int SO_REUSEADDR = 2;
    
    public static final int SO_KEEPALIVE = 9;

    public static final int SO_REUSEPORT = 15;

    public static final int TCP_NODELAY = 1;

    public static final int TCP_CORK = 3;

    public static final int SOCK_NONBLOCK = 00004000;

    public static final int SOCK_CLOEXEC = 02000000;

    public static final short AF_INET = 2;


    static {
        try {
            Class.forName(Native.class.getName());
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("could not load Native");
        }
    }

    public static int newStreamSocket(boolean sockCloExec, boolean sockNonblock) throws IOException {
        int res = newStreamSocketFd(sockCloExec, sockNonblock);
        if(res < 0) {
            Errors.ioException(res);
        }
        return res;
    }

    public static void bind(int fd, Inet4Address address, int port) throws IOException {
        byte[] addressBytes = address.getAddress();
        int res = bind0(fd, addressBytes, port);
        if(res < 0) {
            Errors.ioException(res);
        }
    }

    public static void listen(int fd, int backlog) throws IOException {
        int res = listen0(fd, backlog);
        if(res < 0) {
            Errors.ioException(res);
        }
    }

    public static void setSockOpt(int fd, int level, int optname, int optval) throws IOException {
        int res = setSockOpt0(fd, level, optname, optval);
        if(res < 0) {
            Errors.ioException(res);
        }
    }

    public static int getSockOpt(int fd, int level, int optname) throws IOException {
        int res = getSockOpt0(fd, level, optname);
        if(res < 0) {
            Errors.ioException(res);
        }
        return res;
    }

    public static void connect(int fd, Inet4Address address, int port) throws IOException {
        byte[] addressBytes = address.getAddress();
        int res = connect0(fd, addressBytes, port);
        if(res < 0) {
            Errors.ioException(res);
        }
    }

    public static void shutdown(int fd) throws IOException {
        shutdown(fd, true, true);
    }

    public static void shutdown(int fd, boolean read, boolean write) throws IOException {
        int res = shutdown0(fd, read, write);
        if(res < 0) {
            Errors.ioException(res);
        }
    }

    public static void close(int fd) throws IOException {
        int res = close0(fd);
        if(res < 0) {
            Errors.ioException(res);
        }
    }
    
    private static native int newStreamSocketFd(boolean sockCloExec, boolean sockNonblock);

    private static native int bind0(int fd, byte[] address, int port);

    private static native int listen0(int fd, int backlog);

    private static native int connect0(int fd, byte[] address, int port);

    private static native int shutdown0(int fd, boolean read, boolean write);

    private static native int close0(int fd);

    private static native int getSockOpt0(int fd, int level, int optname);

    private static native int setSockOpt0(int fd, int level, int optname, int optval);

}
