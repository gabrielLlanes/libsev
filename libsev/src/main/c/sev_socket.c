#include <stdlib.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdbool.h>

#include "sev_socket.h"

static int sev_socket(int domain, int type, int protocol) {
    return socket(domain, type, protocol);
}

int sev_socket_streamSocket(int domain, int type, int protocol) {
    int sock = sev_socket(domain, type, protocol);
    if(sock == -1) return -errno;
    return sock;
}

int sev_socket_bind(int fd, const struct sockaddr *addr, socklen_t len) {
    int res = bind(fd, addr, len);
    if(res == -1) {
        return -errno;
    }
    return 0;
}

int sev_socket_listen(int fd, int backlog) {
    int res = listen(fd, backlog);
    if(res == -1) {
        return -errno;
    }
    return 0;
}

int sev_socket_connect(int fd, const struct sockaddr *addr, socklen_t len) {
    int res;
    do {
        res = connect(fd, addr, len);
    } while(res == -1 && errno == EINTR);
    if(res == -1) {
        return -errno;
    }
    return 0;
}

int sev_socket_shutdown(int fd, bool read, bool write) {
    int mode;
    if(read && write) {
        mode = SHUT_RDWR;
    } else if(read) {
        mode = SHUT_RD;
    } else if(write) {
        mode = SHUT_WR;
    } else {
        return -EINVAL;
    }
    int res = shutdown(fd, mode);
    if(res == -1) {
        return -errno;
    }
    return 0;
}

int sev_socket_close(int fd) {
    int res = close(fd);
    if(res == -1) {
        if(errno != EINTR) {
            return -errno;
        }
    }
    return 0;
}

int sev_socket_getSockOpt(int fd, int level, int optname) {
    int optval;
    socklen_t len = sizeof(optval);
    int res = getsockopt(fd, level, optname, &optval, &len);
    if(res == -1) {
        return -errno;
    }
    return optval;
}

int sev_socket_setSockOpt(int fd, int level, int optname, int optval) {
    int res = setsockopt(fd, level, optname, &optval, sizeof(optval));
    if(res == -1) {
        return -errno;
    }
    return 0;
}
