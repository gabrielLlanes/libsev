#ifndef SEV_SOCKET_H_
#define SEV_SOCKET_H_

#include <sys/socket.h>

// call traditional socket functions, but return -errno instead
// of -1 on error, avoid need to capture errno via FFM

int sev_socket_streamSocket(int domain, int type, int protocol);

int sev_socket_bind(int fd, const struct sockaddr *addr, socklen_t len);

int sev_socket_listen(int fd, int backlog);

int sev_socket_connect(int fd, const struct sockaddr *addr, socklen_t len);

int sev_socket_shutdown(int fd, bool read, bool write);

int sev_socket_close(int fd);

int sev_socket_getSockOpt(int fd, int level, int optname);

int sev_socket_setSockOpt(int fd, int level, int optname, int optval);

#endif