#include "sev_jni.h"
#include "sev_buffer.h"

#define SEV_BUFFER_CLASSNAME "io/sev/util/BufferUtil"

static jlong sev_buffer_memoryAddress0
    (JNIEnv *env, jclass clazz, jobject buffer) {
        return (jlong) (*env)->GetDirectBufferAddress(env, buffer);
    }

static const JNINativeMethod method_table[] = {
    {"memoryAddress0", "(Ljava/nio/ByteBuffer;)J", sev_buffer_memoryAddress0}
};

static const jint method_table_size = 1;

jint sev_buffer_JNI_OnLoad(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(env, SEV_BUFFER_CLASSNAME);
    int res = (*env)->RegisterNatives(env, clazz, method_table, method_table_size);
    return res;
}