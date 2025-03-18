#include <stdlib.h>
#include <stddef.h>
#include <stdint.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <stdbool.h>
#include <time.h>

#include "liburing.h"

int sev_uring_queueInit(unsigned entries, long ring, unsigned flags) {
    int res = io_uring_queue_init(entries, (struct io_uring *) ring, flags);
    return res;
}

long sev_uring_getSqe(long ring) {
    struct io_uring_sqe *sqe = io_uring_get_sqe((struct io_uring *) ring);
    return (long) sqe;
}

void sev_uring_sqeSetData64(long sqe, long data) {
    io_uring_sqe_set_data64((struct io_uring_sqe *) sqe, data);
}

void sev_uring_sqeSetData(long sqe, long data) {
    io_uring_sqe_set_data((struct io_uring_sqe *) sqe, (void *) data);
}

void sev_uring_sqeSetFlags(long sqe, unsigned flags) {
    io_uring_sqe_set_flags((struct io_uring_sqe *) sqe, flags);
}

void sev_uring_prepNop(long sqe) {
    io_uring_prep_nop((struct io_uring_sqe *) sqe);
}

void sev_uring_prepAccept(long sqe, int sockfd, struct sockaddr *addr, socklen_t *addrlen, int flags, bool multishot) {
    if(multishot) {
        io_uring_prep_multishot_accept((struct io_uring_sqe *) sqe, sockfd, addr, addrlen, flags);
    } else {
        io_uring_prep_accept((struct io_uring_sqe *) sqe, sockfd, addr, addrlen, flags);
    }
}

void sev_uring_prepConnect(long sqe, int fd, const struct sockaddr *addr, socklen_t addrlen) {
    io_uring_prep_connect((struct io_uring_sqe *) sqe, fd, addr, addrlen);
}

void sev_uring_prepClose(long sqe, int fd) {
    io_uring_prep_close((struct io_uring_sqe *) sqe, fd);
}

void sev_uring_prepShutdown(long sqe, int fd, int how) {
    io_uring_prep_shutdown((struct io_uring_sqe *) sqe, fd, how);
}

void sev_uring_prepRead(long sqe, int fd, void *buf, unsigned nbytes, unsigned long long offset) {
    io_uring_prep_read((struct io_uring_sqe *) sqe, fd, buf, nbytes, offset);
}

void sev_uring_prepWrite(long sqe, int fd, void *buf, unsigned nbytes, unsigned long long offset) {
    io_uring_prep_write((struct io_uring_sqe *) sqe, fd, buf, nbytes, offset);
}

void sev_uring_prepRecv(long sqe , int sockfd, void *buf, size_t len, int flags) {
    io_uring_prep_recv((struct io_uring_sqe *) sqe, sockfd, buf, len, flags);
}

void sev_uring_prepSend(long sqe , int sockfd, void *buf, size_t len, int flags) {
    io_uring_prep_send((struct io_uring_sqe *) sqe, sockfd, buf, len, flags);
}

void sev_uring_prepTimeout(long sqe, struct __kernel_timespec *ts, unsigned count, unsigned flags) {
    io_uring_prep_timeout((struct io_uring_sqe *) sqe, ts, count, flags);
}

void sev_uring_prepPollAdd(long sqe, int fd, unsigned poll_mask) {
    io_uring_prep_poll_add((struct io_uring_sqe *) sqe, fd, poll_mask);
}

void sev_uring_prepCancel64(long sqe, long user_data, int flags) {
    io_uring_prep_cancel64((struct io_uring_sqe *) sqe, user_data, flags);
}

void sev_uring_prepCancel(long sqe, long user_data, int flags) {
    io_uring_prep_cancel((struct io_uring_sqe *) sqe, (void *) user_data, flags);
}

int sev_uring_submit(long ring) {
    return io_uring_submit((struct io_uring *) ring);
}

int sev_uring_submitAndWait(long ring, unsigned wait_nr) {
    return io_uring_submit_and_wait((struct io_uring *) ring, wait_nr);
}

static uint32_t minOfInts(uint32_t a, uint32_t b) {
    return a < b ? a : b;
}

/*

cq ring:
0                        n-1
|......head......tail......|

needs wrapping:

|......tail......head......|

*/

static uint32_t copy_cqes_ready(struct io_uring *ring, struct io_uring_cqe *cqes, uint32_t cqes_len) {
    uint32_t ready = io_uring_cq_ready(ring);
    uint32_t count = minOfInts(ready, cqes_len);
    uint32_t head = *(ring->cq.khead) & ring->cq.ring_mask;

    uint32_t n = minOfInts(ring->cq.ring_entries - head, count);
    struct io_uring_cqe *cqring_head_ptr = ring->cq.cqes + head;
    memcpy(cqes, cqring_head_ptr, n * sizeof(struct io_uring_cqe));
    if(count > n) {
        uint32_t extra = count - n;
        struct io_uring_cqe *cqring_array_ptr = ring->cq.cqes;
        struct io_uring_cqe *cqes_remaining = cqes + n;
        memcpy(cqes_remaining, cqring_array_ptr, extra * sizeof(struct io_uring_cqe));
    }
    io_uring_cq_advance(ring, count);
    return count;
}

static int cq_ring_needs_flush(struct io_uring *ring) {
    return IO_URING_READ_ONCE(*ring->sq.kflags) & (IORING_SQ_CQ_OVERFLOW);
}

//inspired by implementation of zig implementation in std.os.linux.IoUring.cope_cqes()
int sev_uring_copyCqes(long ring, struct io_uring_cqe *cqes, uint32_t cqes_len, uint32_t wait_nr) {
    uint32_t count = copy_cqes_ready((struct io_uring *) ring, cqes, cqes_len);
    if(count > 0) {
        return count;
    }
    if(cq_ring_needs_flush(ring) || wait_nr > 0) {
        int enter = io_uring_enter(((struct io_uring *) ring)->ring_fd, 0, wait_nr, IORING_ENTER_GETEVENTS, NULL);
        if(enter < 0) {
            return enter;
        } else {
            return copy_cqes_ready((struct io_uring *) ring, cqes, cqes_len);
        }
    }
    return 0;
}

void sev_uring_queueExit(long ring) {
    io_uring_queue_exit((struct io_uring *) ring);
}