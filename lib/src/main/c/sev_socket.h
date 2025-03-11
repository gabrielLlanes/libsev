#ifndef SEV_SOCKET_H_
#define SEV_SOCKET_H_

#include <jni.h>
#include <sys/socket.h>

int sev_socket(int domain, int type, int protocol);

int sev_socket_initAddr(JNIEnv *env, jbyteArray address, jint port,
    struct sockaddr_in *addr, socklen_t *addrSize);

jint sev_socket_JNI_OnLoad(JNIEnv *env);

#endif