package io.sev.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.sev.util.errors.UnixException;
import io.sev.util.inet.InetUtil;

import static io.sev.util.errors.UnixException.*;
import static io.sev.util.inet.InetUtil.*;
import static io.sev.socket.Socket.*;

@TestInstance(Lifecycle.PER_CLASS)
public class SocketTest {

    private static Arena arena;

    @BeforeAll
    public static void beforeAll() {
        arena = Arena.ofConfined();
    }

    @Test
    public void streamSocketTest() throws Throwable {
        int fd = (int) socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        assertTrue(fd > 0);
        closeStrict(fd);
    }

    @Test 
    public void bindTest() throws Throwable {
        int fd = (int) socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        Inet4Address localhost = (Inet4Address) InetAddress.getByName("127.0.0.1");
        int port = 8080;
        MemorySegment addr = InetUtil.sockAddrInet4(localhost, port, arena);
        bind(fd, addr, SOCKADDR_IN_SIZE);
        closeStrict(fd);
        System.out.println("BIND TEST");
    }

    @Test
    public void sockOptTest() throws Throwable {
        int fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEADDR, 1);
        int optval = getSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEADDR);
        assertEquals(1, optval);
        closeStrict(fd);
        System.out.println("SOCKOPT TEST");
    }

    @Test
    public void listenTest() throws Throwable {
        int fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        setReuseAddrPort(fd);
        Inet4Address localhost = (Inet4Address) InetAddress.getByName("127.0.0.1");
        int port = 8080;
        MemorySegment addr = InetUtil.sockAddrInet4(localhost, port, arena);
        bind(fd, addr, SOCKADDR_IN_SIZE);
        listen(fd, 1);
        closeStrict(fd);
        System.out.println("LISTEN TEST");
    }

    @Test
    public void connectTest() throws Throwable {
        int fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        setReuseAddrPort(fd);
        Inet4Address localhost =(Inet4Address) InetAddress.getByName("127.0.0.1");
        int port = 8080;
        MemorySegment addr = InetUtil.sockAddrInet4(localhost, port, arena);
        bind(fd, addr, SOCKADDR_IN_SIZE);
        listen(fd, 1);

        int client = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        connect(client, addr, SOCKADDR_IN_SIZE);

        closeStrict(fd);
        closeStrict(client);
        System.out.println("CONNECT TEST");
    }

    @Test
    public void socketShutdownTest() throws Throwable {
        int fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        setReuseAddrPort(fd);
        Inet4Address localhost =(Inet4Address) InetAddress.getByName("127.0.0.1");
        int port = 8080;
        MemorySegment addr = InetUtil.sockAddrInet4(localhost, port, arena);
        bind(fd, addr, SOCKADDR_IN_SIZE);
        listen(fd, 1);

        int client = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        connect(client, addr, SOCKADDR_IN_SIZE);

        shutdownSocket(client);

        closeStrict(fd);
        closeStrict(client);
        System.out.println("SHUTDOWN TEST");
    }

    @Test
    public void ipv6Test() throws Throwable {
        int fd = socket(AF_INET6, SOCK_STREAM | SOCK_CLOEXEC, 0);
        setReuseAddrPort(fd);
        Inet6Address loopback =(Inet6Address) InetAddress.getByName("::1");
        int port = 8080;
        MemorySegment addr = InetUtil.sockAddrInet6(loopback, port, arena);
        bind(fd, addr, SOCKADDR_IN6_SIZE);
        listen(fd, 1);

        int client = socket(AF_INET6, SOCK_STREAM | SOCK_CLOEXEC, 0);
        connect(client, addr, SOCKADDR_IN6_SIZE);

        shutdownSocket(client);

        closeStrict(fd);
        closeStrict(client);
        System.out.println("IPV6 TEST");
    }

    @Test
    public void errorInProgressTest() throws Throwable {
        int fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        MemorySegment addr = sockAddrInet4((Inet4Address) InetAddress.getByName("127.0.0.1"), 8085, arena);
        bind(fd, addr, SOCKADDR_IN_SIZE);

        int clientFd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC | SOCK_NONBLOCK, 0);
        UnixException ex = null;
        try {
            connect(clientFd, addr, SOCKADDR_IN_SIZE);
        } catch(UnixException e) {
            ex = e;
        }
        assertEquals(EINPROGRESS, ex.errno());
        closeStrict(fd);
        closeStrict(clientFd);
    }

    private void setReuseAddrPort(int fd) throws Throwable {
        setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEADDR, 1);
        setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEPORT, 1);
    }

    @AfterAll
    public static void afterAll() {
        arena.close();
    }
    
}
