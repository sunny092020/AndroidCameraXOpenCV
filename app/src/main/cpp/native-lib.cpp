#include <jni.h>
#include <string>
#include <vector>
#include <opencv2/core.hpp>
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/imgproc.hpp"
#include <algorithm>
#include <vector>
#include <iostream>
#include <opencv2/opencv_modules.hpp>
#include <opencv2/core/utility.hpp>
#include <opencv2/features2d.hpp>
#include <unistd.h>

using namespace std;
using namespace cv;

#include <android/log.h>
#define LOG_TAG "VisionUtils/houghLines"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

template <typename T> class Vector2D
{
private:
    T x;
    T y;

public:
    explicit Vector2D(const T& x=0, const T& y=0) : x(x), y(y) {}
    Vector2D(const Vector2D<T>& src) : x(src.x), y(src.y) {}
    virtual ~Vector2D() {}

    // Accessors
    inline T X() const { return x; }
    inline T Y() const { return y; }
    inline T X(const T& x) { this->x = x; }
    inline T Y(const T& y) { this->y = y; }

    // Vector arithmetic
    inline Vector2D<T> operator-() const
    { return Vector2D<T>(-x, -y); }

    inline Vector2D<T> operator+() const
    { return Vector2D<T>(+x, +y); }

    inline Vector2D<T> operator+(const Vector2D<T>& v) const
    { return Vector2D<T>(x+v.x, y+v.y); }

    inline Vector2D<T> operator-(const Vector2D<T>& v) const
    { return Vector2D<T>(x-v.x, y-v.y); }

    inline Vector2D<T> operator*(const T& s) const
    { return Vector2D<T>(x*s, y*s); }

    // Dot product
    inline T operator*(const Vector2D<T>& v) const
    { return x*v.x + y*v.y; }

    // l-2 norm
    inline T norm() const { return sqrt(x*x + y*y); }

    // inner angle (radians)
    static T angle(const Vector2D<T>& v1, const Vector2D<T>& v2)
    {
        return acos( (v1 * v2) / (v1.norm() * v2.norm()) );
    }
};

static double maximum(double number1, double number2, double number3)
{
    return std::max(std::max(number1, number2), number3);
}

#define EPSILON 1E-5

static bool almostEqual(double number1, double number2)
{
    return (std::abs(number1 - number2) <= (EPSILON * maximum(1.0, std::abs(number1), std::abs(number2))));
}

static bool lineIntersection(const cv::Point2f &a1, const cv::Point2f &b1, const cv::Point2f &a2,
                             const cv::Point2f &b2, cv::Point2f &intersection)
{
    double A1 = b1.y - a1.y;
    double B1 = a1.x - b1.x;
    double C1 = (a1.x * A1) + (a1.y * B1);

    double A2 = b2.y - a2.y;
    double B2 = a2.x - b2.x;
    double C2 = (a2.x * A2) + (a2.y * B2);

    double det = (A1 * B2) - (A2 * B1);

    if (!almostEqual(det, 0)) {
        intersection.x = static_cast<float>(((C1 * B2) - (C2 * B1)) / (det));
        intersection.y = static_cast<float>(((C2 * A1) - (C1 * A2)) / (det));

        return true;
    }

    return false;
}

static int noiseLineSingle(Mat &mat, Vec4i &line)
{
    Point pt1 = Point(line[0], line[1]);
    Point pt2 = Point(line[2], line[3]);

    int delta = 2;

    Point pt1_side1 = Point(line[0] + delta, line[1] + delta);
    Point pt2_side1 = Point(line[2] + delta, line[3] + delta);
    Point pt1_side2 = Point(line[0] - delta, line[1] - delta);
    Point pt2_side2 = Point(line[2] - delta, line[3] - delta);

    LineIterator it(mat, pt1, pt2, 8);
    vector<uchar> buf(it.count);

    LineIterator it_side1(mat, pt1_side1, pt2_side1, 8);
    vector<uchar> buf_side1(it_side1.count);

    LineIterator it_side2(mat, pt1_side2, pt2_side2, 8);
    vector<uchar> buf_side2(it_side2.count);

    uchar total_diff = 0;

    for(int i = 0; i < it.count; i++, ++it)
    {
        if(i>100) break;
        buf[i] = *(const uchar*)*it;
        uchar color = buf[i];

        buf_side1[i] = *(const uchar*)*it_side1;
        uchar color_side1 = buf_side1[i];

        buf[i] = *(const uchar*)*it;
        uchar color_side2 = buf_side2[i];

//        LOGD("color %u", color);
//        LOGD("color_side1 %u", color_side1);
//        LOGD("color_side2 %u", color_side2);

        total_diff+=abs(color_side2-color_side1);
    }

//    LOGD("total_diff %u", total_diff);

    return total_diff;
}

static int noiseLine(Mat &src_mat, Vec4i &line)
{
    Point pt1 = Point(line[0], line[1]);
    Point pt2 = Point(line[2], line[3]);

    int delta = 2;

    Point pt1_side1 = Point(line[0] + delta, line[1] + delta);
    Point pt2_side1 = Point(line[2] + delta, line[3] + delta);
    Point pt1_side2 = Point(line[0] - delta, line[1] - delta);
    Point pt2_side2 = Point(line[2] - delta, line[3] - delta);

    LineIterator it(src_mat, pt1, pt2, 8);
    vector<Vec3b> buf(it.count);

    LineIterator it_side1(src_mat, pt1_side1, pt2_side1, 8);
    vector<Vec3b> buf_side1(it_side1.count);

    LineIterator it_side2(src_mat, pt1_side2, pt2_side2, 8);
    vector<Vec3b> buf_side2(it_side2.count);

    uchar total_diff = 0;

    for(int i = 0; i < it.count; i++, ++it)
    {
        if(i>100) break;
        buf[i] = *(const Vec3b*)*it;
        uchar blue = buf[i].val[0];
        uchar green = buf[i].val[1];
        uchar red = buf[i].val[2];

        LOGD("blue %u", blue);
        LOGD("green %u", green);
        LOGD("red %u", red);

        buf_side1[i] = *(const Vec3b*)*it_side1;
        uchar blue_side1 = buf_side1[i].val[0];
        uchar green_side1 = buf_side1[i].val[1];
        uchar red_side1 = buf_side1[i].val[2];
        LOGD("blue_side1 %u", blue_side1);
        LOGD("green_side1 %u", green_side1);
        LOGD("red_side1 %u", red_side1);

        buf_side2[i] = *(const Vec3b*)*it_side2;
        uchar blue_side2 = buf_side2[i].val[0];
        uchar green_side2 = buf_side2[i].val[1];
        uchar red_side2 = buf_side2[i].val[2];

        LOGD("blue_side2 %u", blue_side2);
        LOGD("green_side2 %u", green_side2);
        LOGD("red_side2 %u", red_side2);

        total_diff+=abs(blue_side2-blue_side1) + abs(green_side2 - green_side1) + abs(red_side2 - red_side1);

    }

    return total_diff;
}

static void houghLines(Mat &src_mat, Mat &in_mat, Mat &debug_mat, vector<Vec4i> &vertical_lines, vector<Vec4i> &horizontal_lines, vector<Point2f> &intersections)
{
    Mat canny = Mat();

    double lowThres = 50, highthres = lowThres*3;
    Canny(in_mat, canny, lowThres, highthres, 3);

    in_mat.release();

    // extract lines from the edge image
    vector<Vec4i> lines_p = vector<Vec4i>(); // will hold the results of the detection
    int start_min_line = 50;
    HoughLinesP(canny, lines_p, 1, CV_PI/180, 50, start_min_line, 10 ); // runs the actual detection

    LOGD(" END -- lines_p num %d", lines_p.size());

    // Draw the lines
    for( size_t i = 0; i < lines_p.size(); i++ )
    {
        Vec4i l = lines_p[i];

        if(noiseLineSingle(canny, l) < 150) continue;

        double delta_x = l[0] - l[2], delta_y = l[1] - l[3];

        // put horizontal lines and vertical lines respectively
        if (abs(delta_x) > abs(delta_y)) {
            vertical_lines.push_back(l);
        } else {
            horizontal_lines.push_back(l);
        }
    }

    canny.release();


//    for( size_t i = 0; i < local_vertical_lines.size(); i++ )
//    {
//        Vec4i vertical_l = local_vertical_lines[i];
//
//        Vector2D<double> p1(vertical_l[0], vertical_l[1]);
//        Vector2D<double> p2(vertical_l[2], vertical_l[3]);
//
//        for( size_t j = 0; j < local_horizontal_lines.size(); j++ )
//        {
//            Vec4i horizontal_l = local_horizontal_lines[j];
//            Vector2D<double> p3(horizontal_l[0], horizontal_l[1]);
//            Vector2D<double> p4(horizontal_l[2], horizontal_l[3]);
//
//            double rad = Vector2D<double>::angle(p2-p1, p4-p3);
//
//            double deg = rad * 180.0 / M_PI;
//
//            if(deg < 60 || deg > 120) continue;
//
//            Point2f intersection = Point2f();
//            bool has_intersection = lineIntersection(
//                    Point2f(vertical_l[0], vertical_l[1]),
//                    Point2f(vertical_l[2], vertical_l[3]),
//                    Point2f(horizontal_l[0], horizontal_l[1]),
//                    Point2f(horizontal_l[2], horizontal_l[3]),
//                    intersection);
//
//            if(has_intersection) intersections.push_back(intersection);
//        }
//    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ami_icamdocscanner_activities_ImageDoneActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */)
{
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

struct target_less
{
    template<class It>
    bool operator()(It const &a, It const &b) const {
        if(a->x == b->x) return a->y < b->y;
        return a->x < b->x;
    }
};
struct target_equal
{
    template<class It>
    bool operator()(It const &a, It const &b) const {
        return a->x == b->x && a->y == b->y;
    }
};
template<class It> It uniquify(It begin, It const end)
{
    std::vector<It> v;
    v.reserve(static_cast<size_t>(std::distance(begin, end)));
    for (It i = begin; i != end; ++i)
    { v.push_back(i); }
    std::sort(v.begin(), v.end(), target_less());
    v.erase(std::unique(v.begin(), v.end(), target_equal()), v.end());
    std::sort(v.begin(), v.end());
    size_t j = 0;
    for (It i = begin; i != end && j != v.size(); ++i)
    {
        if (i == v[j])
        {
            using std::iter_swap; iter_swap(i, begin);
            ++j;
            ++begin;
        }
    }
    return begin;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ami_icamdocscanner_helpers_VisionUtils_findContours(
        JNIEnv* env,
        jclass /* this */,
        jlong src_mat_addr,
        jlong debug_mat_addr) {
    Mat src_mat = *(Mat*)src_mat_addr;
    Mat debug_mat = *(Mat*)debug_mat_addr;

    /* get four outline edges of the document */
    // get edges of the image
    Mat gray = Mat();
    cvtColor(src_mat, gray, COLOR_RGB2GRAY);

    Mat hsv = Mat();
    cvtColor(src_mat, hsv, COLOR_RGB2HSV);

    vector<Mat> channels;
    split(hsv, channels);
    hsv.release();

    Mat H = channels[0];
    Mat S = channels[1];
    Mat V = channels[2];

    Mat notGray = Mat();
    Mat notH = Mat();
    Mat notS = Mat();
    Mat notV = Mat();

    bitwise_not(gray, notGray);
    bitwise_not(H, notH);
    bitwise_not(S, notS);
    bitwise_not(V, notV);


    vector<Vec4i> vertical_lines = vector<Vec4i>();
    vector<Vec4i> horizontal_lines = vector<Vec4i>();
    vector<Point2f> intersections = vector<Point2f>();

    houghLines(src_mat, gray, debug_mat, vertical_lines, horizontal_lines, intersections);
    LOGD("gray vertical_lines size: %d", vertical_lines.size());
    LOGD("gray horizontal_lines size: %d", horizontal_lines.size());

//    houghLines(src_mat,H, debug_mat, vertical_lines, horizontal_lines, intersections);
//    LOGD("H vertical_lines size: %d", vertical_lines.size());
//    LOGD("H horizontal_lines size: %d", horizontal_lines.size());
//
//    houghLines(src_mat,S, debug_mat, vertical_lines, horizontal_lines, intersections);
//    LOGD("S vertical_lines size: %d", vertical_lines.size());
//    LOGD("S horizontal_lines size: %d", horizontal_lines.size());
//
//    houghLines(src_mat,V, debug_mat, vertical_lines, horizontal_lines, intersections);
//    LOGD("V vertical_lines size: %d", vertical_lines.size());
//    LOGD("V horizontal_lines size: %d", horizontal_lines.size());
//
//    houghLines(src_mat, notGray, debug_mat, vertical_lines, horizontal_lines, intersections);
//    LOGD("notGray vertical_lines size: %d", vertical_lines.size());
//    LOGD("notGray horizontal_lines size: %d", horizontal_lines.size());
//
//    houghLines(src_mat, notH, debug_mat, vertical_lines, horizontal_lines, intersections);
//    LOGD("notH vertical_lines size: %d", vertical_lines.size());
//    LOGD("notH horizontal_lines size: %d", horizontal_lines.size());
//
//    houghLines(src_mat, notS, debug_mat, vertical_lines, horizontal_lines, intersections);
//    LOGD("notS vertical_lines size: %d", vertical_lines.size());
//    LOGD("notS horizontal_lines size: %d", horizontal_lines.size());
//
//    houghLines(src_mat, notV, debug_mat, vertical_lines, horizontal_lines, intersections);
//    LOGD("notV vertical_lines size: %d", vertical_lines.size());
//    LOGD("notV horizontal_lines size: %d", horizontal_lines.size());
//

//    for( size_t i = 0; i < intersections.size(); i++ )
//    {
//        Point2f intersection = intersections[i];
//        circle( debug_mat,
//                Point_(intersection.x, intersection.y),
//                10,
//                Scalar( 255, 0, 0 ),
//                FILLED,
//                LINE_8 );
//    }

    for( size_t i = 0; i < vertical_lines.size(); i++ )
    {
        Vec4i vertical_l = vertical_lines[i];

        line( debug_mat,
              Point(vertical_l[0], vertical_l[1]),
              Point(vertical_l[2], vertical_l[3]),
              Scalar(255, 0, 0),
              10);

    }

    for( size_t i = 0; i < horizontal_lines.size(); i++ )
    {
        Vec4i horizontal_l = horizontal_lines[i];

        line( debug_mat,
              Point(horizontal_l[0], horizontal_l[1]),
              Point(horizontal_l[2], horizontal_l[3]),
              Scalar(255, 0, 0),
              10);

    }

    src_mat.release();

}
