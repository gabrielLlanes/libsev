package io.sev.socket;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static io.sev.util.unix.UnixException.unixException;
import static java.lang.foreign.ValueLayout.ADDRESS;

import io.sev.Native;
import io.sev.util.unix.UnixException;

public class Socket {

    private static final MethodHandle socketHandle;

    private static final MethodHandle bindHandle;

    private static final MethodHandle listenHandle;

    private static final MethodHandle connectHandle;

    private static final MethodHandle shutdownSocketHandle;

    private static final MethodHandle closeHandle;

    private static final MethodHandle getSockOptHandle;

    private static final MethodHandle setSockOptHandle;

    static {
        try {
            Class.forName(Native.class.getName());
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("could not load Native");
        }
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        MemorySegment socketSegment = lookup.findOrThrow("sev_socket_streamSocket");
        FunctionDescriptor socketDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
        socketHandle = linker.downcallHandle(socketSegment, socketDescriptor);

        MemorySegment bindSegment = lookup.findOrThrow("sev_socket_bind");
        FunctionDescriptor bindDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT);
        bindHandle = linker.downcallHandle(bindSegment, bindDescriptor);

        MemorySegment listenSegment = lookup.findOrThrow("sev_socket_listen");
        FunctionDescriptor listenDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT);
        listenHandle = linker.downcallHandle(listenSegment, listenDescriptor);

        MemorySegment connectSegment = lookup.findOrThrow("sev_socket_connect");
        FunctionDescriptor connectDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT);
        connectHandle = linker.downcallHandle(connectSegment, connectDescriptor);

        MemorySegment shutdownSocketSegment = lookup.findOrThrow("sev_socket_shutdown");
        FunctionDescriptor shutdownSocketDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_BOOLEAN, JAVA_BOOLEAN);
        shutdownSocketHandle = linker.downcallHandle(shutdownSocketSegment, shutdownSocketDescriptor);

        MemorySegment closeSegment = lookup.findOrThrow("sev_socket_close");
        FunctionDescriptor closeDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT);
        closeHandle = linker.downcallHandle(closeSegment, closeDescriptor);

        MemorySegment getSockOptSegment = lookup.findOrThrow("sev_socket_getSockOpt");
        FunctionDescriptor getSockOptDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
        getSockOptHandle = linker.downcallHandle(getSockOptSegment, getSockOptDescriptor);

        MemorySegment setSockOptSegment = lookup.findOrThrow("sev_socket_setSockOpt");
        FunctionDescriptor setSockOptDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
        setSockOptHandle = linker.downcallHandle(setSockOptSegment, setSockOptDescriptor);
    }

    private static Object invokeWithArgumentsUnchecked(MethodHandle handle, Object... args) {
        Object result = null;
        try {
            result = handle.invokeWithArguments(args);
            return result;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int socket(int domain, int type, int protocol) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(socketHandle, domain, type, protocol);
        if(res < 0) {
            unixException(res);
        }
        return res;
    }

    public static void bind(int fd, MemorySegment addr, int len) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(bindHandle, fd, addr, len);
        if(res < 0) {
            unixException(res);
        }
    }

    public static void closeStrict(int fd) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(closeHandle, fd);
        if(res < 0) {
            unixException(res);
        }
    }

    public static void close(int fd) {
        invokeWithArgumentsUnchecked(closeHandle, fd);
    }

    public static void listen(int fd, int backlog) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(listenHandle, fd, backlog);
        if(res < 0) {
            unixException(res);
        }
    }

    public static void connect(int fd, MemorySegment addr, int len) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(connectHandle, fd, addr, len);
        if(res < 0) {
            unixException(res);
        }
    }

    public static void shutdownSocket(int fd, boolean read, boolean write) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(shutdownSocketHandle, fd, read, write);
        if(res < 0) {
            unixException(res);
        }
    }

    public static void shutdownSocket(int fd) throws UnixException {
        shutdownSocket(fd, true, true);
    }

    public static void setSockOpt(int fd, int level, int optname, int optval) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(setSockOptHandle, fd, level, optname, optval);
        if(res < 0) {
            unixException(res);
        }
    }

    public static int getSockOpt(int fd, int level, int optname) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(getSockOptHandle, fd, level, optname);
        if(res < 0) {
            unixException(res);
        }
        return res;
    }
}
