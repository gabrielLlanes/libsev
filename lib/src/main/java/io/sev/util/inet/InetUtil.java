package io.sev.util.inet;

import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.nio.ByteOrder;

import static io.sev.util.unix.Macros.*;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;

public class InetUtil {
    
    private static final ByteOrder NETWORK_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

    /*
     * struct sockaddr_in {
     *   sa_family_t sin_family;
     *   in_port_t sin_port;
     *   struct in_addr {in_addr_t s_addr;} sin_addr;
     *   unsigned char sin_zero[8];
     * }
     */
    private static final StructLayout SOCKADDR_IN_LAYOUT = structLayout(
                                                            JAVA_SHORT,
                                                            JAVA_SHORT.withOrder(NETWORK_BYTE_ORDER),
                                                            JAVA_INT.withOrder(NETWORK_BYTE_ORDER),
                                                            sequenceLayout(8L, JAVA_BYTE));

    private static final VarHandle inet4FamilyHandle = SOCKADDR_IN_LAYOUT.varHandle(PathElement.groupElement(0));
    private static final VarHandle inet4PortHandle = SOCKADDR_IN_LAYOUT.varHandle(PathElement.groupElement(1));
    private static final VarHandle inet4AddrHandle = SOCKADDR_IN_LAYOUT.varHandle(PathElement.groupElement(2));

    /*
     * create a MemorySegment of a struct sockaddr_in to be used
     * for native functions requiring (struct sockaddr_in *) or (struct sockaddr *)
     */
    public static MemorySegment sockAddrInet4(Inet4Address addr, int port, SegmentAllocator allocator) {
        MemorySegment sockAddrSegment = allocator.allocate(SOCKADDR_IN_LAYOUT);
        inet4FamilyHandle.set(sockAddrSegment, 0L, (short) AF_INET);
        inet4PortHandle.set(sockAddrSegment, 0L, (short) port);
        int _addr = 0;
        byte[] addrBytes = addr.getAddress();
        _addr |= addrBytes[0] << 24;
        _addr |= addrBytes[1] << 16;
        _addr |= addrBytes[2] << 8;
        _addr |= addrBytes[3];
        inet4AddrHandle.set(sockAddrSegment, 0L, _addr);
        return sockAddrSegment;
    }

    /*
     * struct sockaddr_in6 {
     *   sa_family_t sin6_family;
     *   in_port_t sin6_port;
     *   uint32_t sin6_flowinfo;
     *   struct in6_addr sin6_addr;
     *   uint32_t sin6_scope_id;
     * }
     */
    public static final StructLayout SOCKADDR_IN6_LAYOUT = structLayout(
                                                            JAVA_SHORT,
                                                            JAVA_SHORT.withOrder(NETWORK_BYTE_ORDER),
                                                            JAVA_INT.withOrder(NETWORK_BYTE_ORDER),
                                                            sequenceLayout(16L, JAVA_BYTE),
                                                            JAVA_INT.withOrder(NETWORK_BYTE_ORDER));

    private static final VarHandle inet6FamilyHandle = SOCKADDR_IN6_LAYOUT.varHandle(PathElement.groupElement(0));
    private static final VarHandle inet6PortHandle = SOCKADDR_IN6_LAYOUT.varHandle(PathElement.groupElement(1));
    private static final VarHandle inet6AddrHandle = SOCKADDR_IN6_LAYOUT.varHandle(PathElement.groupElement(3), PathElement.sequenceElement());

    /*
     * create a MemorySegment of a struct sockaddr_in6 to be used
     * for native functions requiring (struct sockaddr_in6 *) or (struct sockaddr *)
     */
    public static MemorySegment sockAddrInet6(Inet6Address addr, int port, SegmentAllocator allocator) {
        MemorySegment sockAddrSegment = allocator.allocate(SOCKADDR_IN6_LAYOUT);
        inet6FamilyHandle.set(sockAddrSegment, 0L, (short) AF_INET6);
        inet6PortHandle.set(sockAddrSegment, 0L, (short) port);
        byte[] addrBytes = addr.getAddress();
        for(int i = 0; i < addrBytes.length; i++) {
            inet6AddrHandle.set(sockAddrSegment, 0L, i, addrBytes[i]);
        }
        return sockAddrSegment;
    }

}
