#include <jni.h>
#include <string>
#include <vector>
#include <opencv2/core.hpp>

#include <android/log.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_ami_icamdocscanner_activities_ImageDoneActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_ami_icamdocscanner_helpers_VisionUtils_houghLines(
        JNIEnv* env,
        jobject /* this */,
        jlong mat) {

}