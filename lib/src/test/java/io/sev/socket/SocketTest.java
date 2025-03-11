package io.sev.socket;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

public class SocketTest {

    @Test
    public void newStreamSocketFdTest() throws IOException {
        int fd = Socket.newStreamSocket(true, false);
        assertTrue(fd > 0);
    }

    @Test
    public void sockOptTest() throws IOException {
        int fd = Socket.newStreamSocket(true, false);
        Socket.setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEADDR, 1);
        int optval = Socket.getSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEADDR);
        assertEquals(1, optval);
        System.out.println("SOCKOPT TEST");
    }

    @Test
    public void bindTest() throws IOException {
        int fd = Socket.newStreamSocket(true, false);
        setReuseAddrPort(fd);
        Inet4Address inet4Address =(Inet4Address) InetAddress.getByName("localhost");
        Socket.bind(fd, inet4Address, 8080);
        System.out.println("BIND TEST");
    }

    @Test
    public void listenTest() throws IOException {
        int fd = Socket.newStreamSocket(true, false);
        setReuseAddrPort(fd);
        Inet4Address inet4Address =(Inet4Address) Inet4Address.getByName("localhost");
        Socket.bind(fd, inet4Address, 8080);
        Socket.listen(fd, 16);
        System.out.println("LISTEN TEST");
    }

    @Test
    public void connectTest() throws IOException {
        int fd = Socket.newStreamSocket(true, false);
        setReuseAddrPort(fd);
        Inet4Address inet4Address =(Inet4Address) Inet4Address.getByName("localhost");
        Socket.bind(fd, inet4Address, 8081);
        Socket.listen(fd, 16);
        int client = Socket.newStreamSocket(true, false);
        Socket.connect(client, inet4Address, 8081);
        System.out.println("CONNECT TEST");
    }

    @Test
    public void closeTest() throws IOException {
        int fd = Socket.newStreamSocket(true, false);
        setReuseAddrPort(fd);
        Inet4Address inet4Address =(Inet4Address) Inet4Address.getByName("localhost");
        Socket.bind(fd, inet4Address, 8082);
        Socket.listen(fd, 16);
        int client = Socket.newStreamSocket(true, false);
        Socket.connect(client, inet4Address, 8082);
        Socket.close(client);
        System.out.println("CLOSE TEST");
    }

    @Test
    public void shutdownTest() throws IOException {
        int fd = Socket.newStreamSocket(true, false);
        setReuseAddrPort(fd);
        Inet4Address inet4Address =(Inet4Address) Inet4Address.getByName("localhost");
        Socket.bind(fd, inet4Address, 8083);
        Socket.listen(fd, 16);
        int client = Socket.newStreamSocket(true, false);
        Socket.connect(client, inet4Address, 8083);
        Socket.shutdown(client, true, true);
        System.out.println("SHUTDOWN TEST");
    }

    private void setReuseAddrPort(int fd) throws IOException {
        Socket.setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEADDR, 1);
        System.out.println("SET SO_REUSEADDR FOR " + fd);
        Socket.setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEPORT, 1);
        System.out.println("SET SO_REUSEPORT FOR " + fd);
    }
    
}
