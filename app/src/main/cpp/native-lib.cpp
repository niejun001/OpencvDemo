#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>

using namespace std;
using namespace cv;

extern "C" JNIEXPORT jstring JNICALL
Java_com_justcode_clanugragedemo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_justcode_clanugragedemo_MainActivity_addText2Picture(JNIEnv *env,
                                                              jobject, jintArray pixels_,
                                                              jint w, jint h, jstring textString) {
    const char *text = env->GetStringUTFChars(textString, 0);
    string content = text;

    jint *pixels = env->GetIntArrayElements(pixels_, NULL);
    if (pixels == NULL) {
        return NULL;
    }

    Mat src(h, w, CV_8UC4, pixels);
    int width = src.cols;
    int height = src.rows;
    int margin = 10;
    int baseline;
    Size srcSize = getTextSize(content, FONT_HERSHEY_COMPLEX, 2, 2, &baseline);
    cv::Point point;
    point.x = width - srcSize.width - margin;
    point.y = height - margin;
    //Scalar BGR
    putText(src, content, point, FONT_HERSHEY_COMPLEX, 2, cv::Scalar(94, 206, 165, 255), 2, 8, 0);
    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, pixels);
    env->ReleaseIntArrayElements(pixels_, pixels, 0);
    env->ReleaseStringUTFChars(textString, text);
    return result;
}