#include <jni.h>
#include <stdio.h>
#include <sys/stat.h>
#include "com_netease_util_InodeUtil.h"

/*
 * Class:     com_netease_util_InodeUtil
 * Method:    getInode
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_netease_util_InodeUtil_getInode
(JNIEnv *env, jclass cls, jstring path) {
    jlong inode = -1;
    const char *cpath = env->GetStringUTFChars(path, 0);
    struct stat statbuf;
    if (stat(cpath, &statbuf) != -1) {
        inode = (jlong)statbuf.st_ino;
    }
    
    env->ReleaseStringUTFChars(path, cpath);
    
    return inode;
}