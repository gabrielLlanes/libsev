package io.sev.uring;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

import org.junit.Test;

import io.sev.socket.Socket;
import io.sev.util.Buffer;

public class IOUringTest {

    @Test
    public void queueInitTest() throws IOUringException {
        IOUring ring = IOUring.init();
        long ringAddress = ring.ringAddress();
        System.out.println("RING ADDRESS: " + Long.toHexString(ringAddress));
        assertTrue(ringAddress > 0);
    }

    @Test
    public void getSqeTest() throws IOUringException {
        IOUring ring = IOUring.init();
        long sqeAddress = ring.getSqe();
        System.out.println("SQE ADDRESS: " + Long.toHexString(sqeAddress));
        assertTrue(sqeAddress > 0);
    }

    @Test
    public void acceptConnect() throws Exception {
        int fd = Socket.newStreamSocket(true, false);
        setReuseAddrPort(fd);
        int port = 8090;
        Inet4Address localhost =(Inet4Address) Inet4Address.getByName("127.0.0.1");
        Socket.bind(fd, localhost, port);
        Socket.listen(fd, 1);

        IOUring ring = IOUring.init();

        long acceptSqe = ring.getSqe();
        IOUring.setSqeData(acceptSqe, 100L, false);
        IOUring.prepAccept(acceptSqe, fd, 0, false);
        
        // int submitted1 = ring.submit();
        // assertEquals(submitted1, 1);
        // System.out.println("SUBMITTED 1 SQE");

        // Thread.sleep(10);

        int clientFd = Socket.newStreamSocket(true, false);
        long connectSqe = ring.getSqe();
        IOUring.setSqeData(connectSqe, 101L, false);
        Buffer inet4Address = Buffer.initInet4(localhost, port);
        IOUring.prepConnect(connectSqe, clientFd, inet4Address);

        int submitted2 = ring.submit();
        assertEquals(2, submitted2);
        System.out.println("SUBMITTED 2 SQE");

        // Thread.sleep(10);

        Buffer buf = new Buffer(256);
        int copied = 0;
        while(copied < 2) {
            int currCopied = ring.copyCqes(buf, 1);
            copied += currCopied;
            System.out.printf("Processing %d CQE\n", currCopied);
            printData(buf);
        }
        assertEquals(2, copied);

        //int enter = ring.enter(0, 1, IOUring.IORING_ENTER_GETEVENTS);
        // assertEquals(enter, 2);
        System.out.println(inet4Address.address());
    }

    private void printData(Buffer buf) {
        ByteBuffer b = buf.memory();
        while(b.hasRemaining()) {
            System.out.print(b.getInt() + " ");
        }
        System.out.println();
    }

    private void setReuseAddrPort(int fd) throws IOException {
        Socket.setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEADDR, 1);
        Socket.setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEPORT, 1);
    }
}
