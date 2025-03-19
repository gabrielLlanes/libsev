#ifndef SEV_URING_H_
#define SEV_URING_H_

#include <stddef.h>
#include <stdint.h>
#include <sys/socket.h>
#include <stdbool.h>
#include <time.h>

#include "liburing.h"

int sev_uring_queueInit(unsigned entries, long ring, unsigned flags);

long sev_uring_getSqe(long ring);

void sev_uring_sqeSetData64(long sqe, long data);

void sev_uring_sqeSetData(long sqe, long data);

void sev_uring_sqeSetFlags(long sqe, unsigned flags);

void sev_uring_prepNop(long sqe);

void sev_uring_prepAccept(long sqe, int sockfd, struct sockaddr *addr, socklen_t *addrlen, int flags, bool multishot);

void sev_uring_prepConnect(long sqe, int fd, const struct sockaddr *addr, socklen_t addrlen);

void sev_uring_prepClose(long sqe, int fd);

void sev_uring_prepShutdown(long sqe, int fd, int how);

void sev_uring_prepRead(long sqe, int fd, void *buf, unsigned nbytes, unsigned long long offset);

void sev_uring_prepWrite(long sqe, int fd, void *buf, unsigned nbytes, unsigned long long offset);

void sev_uring_prepRecv(long sqe , int sockfd, void *buf, size_t len, int flags);

void sev_uring_prepSend(long sqe , int sockfd, void *buf, size_t len, int flags);

void sev_uring_prepTimeout(long sqe, struct __kernel_timespec *ts, unsigned count, unsigned flags);

void sev_uring_prepPollAdd(long sqe, int fd, unsigned poll_mask);

void sev_uring_prepCancel64(long sqe, long user_data, int flags);

void sev_uring_prepCancel(long sqe, long user_data, int flags);

int sev_uring_submit(long ring);

int sev_uring_submitAndWait(long ring, unsigned wait_nr);

int sev_uring_copyCqes(long ring, struct io_uring_cqe *cqes, uint32_t cqes_len, uint32_t wait_nr);

void sev_uring_queueExit(long ring);

#endif