package com.example.segemnts;

import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.utils.Converters;

import static android.content.ContentValues.TAG;

class ColorBlobDetector {

    private Map<String, Integer> dictionary = new HashMap<>();

    public static String join(int[] arr, String separator) {
        if (null == arr || 0 == arr.length) return "";
        StringBuilder sb = new StringBuilder(256);
        sb.append(arr[0]);
        for (int i = 1; i < arr.length; i++) sb.append(separator).append(arr[i]);
        return sb.toString();
    }

    void initDictionary() {
        dictionary.put("1110111", 0);
        dictionary.put("0010010", 1);
        dictionary.put("1011110", 2);
        dictionary.put("1011011", 3);
        dictionary.put("0111010", 4);
        dictionary.put("1101011", 5);
        dictionary.put("1101111", 6);
        dictionary.put("1010010", 7);
        dictionary.put("1111111", 8);
        dictionary.put("1111011", 9);
    }

    Mat getMask(int id, Mat orig, int x, int y, int w, int h) {
        Mat mask = Mat.zeros(orig.rows(), orig.cols(), CvType.CV_8UC1);
        Point from = new Point(0, 0);
        Point to = new Point(0, 0);
        switch (id) {
            case 0:
                from = new Point(15, 0);
                to = new Point(w - 15, 15);
                break;
            case 1:
                from = new Point(0, 15);
                to = new Point(15, (h / 2.0) - (15 / 2.0));
                break;
            case 2:
                from = new Point(w - 15, 15);
                to = new Point(w, (h / 2.0) - (15 / 2.0));
                break;
            case 3:
                from = new Point(15, (h / 2.0) - (15 / 2.0) - 1);
                to = new Point(w - 15, (h / 2.0) + (15 / 2.0) + 1);
                break;
            case 4:
                from = new Point(0, (h / 2.0) + (15 / 2.0));
                to = new Point(15, h - 15);
                break;
            case 6:
                to = new Point(w - 15, (h / 2.0) + (15 / 2.0));
                break;
            case 5:
                from = new Point(15, h);
                to = new Point(w - 15, h - 15);
                break;
        }
        Imgproc.rectangle(mask, new Point(from.x + x, from.y + y), new Point(to.x + x, to.y + y), new Scalar(255), -1);
        return mask;
    }

    Mat getScreen(Mat img) {
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
        Mat edged = new Mat();
        Imgproc.Canny(blurred, edged, 50, 200, 255);

        List<MatOfPoint> cnts = new ArrayList<>();
        Mat H = new Mat();
        Imgproc.findContours(edged, cnts, H, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint displayCnt = null;

        for (Mat c : cnts) {
            double peri = Imgproc.arcLength((MatOfPoint2f) c, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(((MatOfPoint) c).toArray()), approx, 0.02d * peri, true);
            if (approx.total() == 4) {
                displayCnt = (MatOfPoint) c;
                break;
            }
        }
        List<MatOfPoint> cnt = new ArrayList<>();
        cnt.add(displayCnt);
        Imgproc.drawContours(img, cnt, -1, new Scalar(0, 0, 255), 4);
        return edged;
    }

    Mat process(Mat orig) {
//        orig = getScreen(orig);
//        orig = imageProcessor(orig);
//        Mat test = extract(orig);
        float exp = (float) 1.6;
        int blur = 27;
        int threshold = 87;
        int adjustment = 5;
        int erode = 3;
        int iterations = 3;

        Mat expose = new Mat();
        Scalar S = new Scalar(exp, exp, exp);
        Core.multiply(orig, S, expose);

        int x = 100;
        int y = 100;
        int w = 100;
        int h = 100;

        expose = expose.submat(x, x + h, x, x + w);

        Mat gray = new Mat();
        Imgproc.cvtColor(expose, gray, Imgproc.COLOR_BGR2GRAY);

        Mat cropped = new Mat();
        Size s = new Size(blur, (int) 1.5 * blur);
        Imgproc.blur(gray, cropped, s);

        Mat cropped_threshold = new Mat();
        Imgproc.adaptiveThreshold(cropped, cropped_threshold, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, threshold, adjustment);
        Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_GRAY2BGR);

        Mat karnel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(erode, erode));
        Mat inverse = new Mat();
        Imgproc.erode(cropped_threshold, inverse, karnel, iterations);
        Imgproc.GaussianBlur(inverse, inverse, new Size(9, 9), 0);
//        Imgproc.cvtColor(inverse, inverse, Imgproc.COLOR_BGR2GRAY);
//
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hr = new Mat();
        Imgproc.findContours(inverse, contours, hr, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

//        float avgWidth = 0;
        float maxWidth = 0;
//        float avgHeight = 0;
//        float avgArea = 0;
//        for (Mat contour : contours) {
//            float area = (float) Imgproc.contourArea(contour);
//            avgArea += area;
//        }
//        avgArea /= contours.size();
        List<MatOfPoint> potetial_digits = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Rect sizes = Imgproc.boundingRect(contour);
            if (sizes.width / sizes.height <= .7 && sizes.width * sizes.height > 900 && sizes.height > sizes.width) {
                potetial_digits.add(contour);
//                avgWidth += sizes.width;
//                avgHeight += sizes.height;
                if (sizes.width > maxWidth) {
                    maxWidth = sizes.width;
                }
            }
        }
        contours.clear();
//        avgWidth /= potentialdigits.size();
//        avgHeight /= potentialdigits.size();

        if (potetial_digits.size() > 0) {
            for (MatOfPoint cnt : potetial_digits) {
                Rect sizes = Imgproc.boundingRect(cnt);
                if (sizes.height > maxWidth) {
                    Point from = new Point(sizes.x + sizes.width, sizes.y);
                    Point to = new Point(sizes.x + sizes.width - (int) maxWidth, sizes.y + sizes.height);
                    Imgproc.rectangle(orig, from, to, new Scalar(255), 3);
                    int[] segments = new int[7];
                    for (int i = 0; i < 7; i++) {
                        try {
                            Mat mask = getMask(i, inverse, sizes.x, sizes.y, (int) maxWidth, (int) sizes.height);
                            Mat segment = new Mat();
                            Core.bitwise_or(inverse, segment, mask);
                            float num = Core.countNonZero(segment);
                            float total = Core.countNonZero(mask);
                            float percent = (float) num / total;
                            if (percent > 0.1) {
                                segments[i] = 1;
                            } else {
                                segments[i] = 0;
                            }
                        } catch (Exception ignored) {

                        }
                    }

                    Log.d(TAG, Arrays.toString(segments));
                }
            }
        }
        w = 300;
        h = 150;
        Rect rect = new Rect((orig.width() - w) / 2, (orig.height() - h) / 2, w, h);

        Mat cropedMat = new Mat(orig, rect);
        cropedMat.copyTo(orig.rowRange(0, cropedMat.rows()).colRange(0, cropedMat.cols()));
        Imgproc.rectangle(orig, new Point((orig.width() - w) / 2, (orig.height() - h) / 2), new Point(w + (orig.width() - w) / 2, h + (orig.height() - h) / 2), new Scalar(0, 255, 0), 3);
        return orig;
    }

    Mat extract(Mat orig) {
        Mat gray = new Mat();
        Imgproc.cvtColor(orig, gray, Imgproc.COLOR_BGR2GRAY);
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
        Mat edged = new Mat();
        Imgproc.Canny(blurred, edged, 100, 200, 3);

        List<MatOfPoint> cnts = new ArrayList<>();
        Mat H = new Mat();
        Imgproc.findContours(edged, cnts, H, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint displayCnt = null;

        for (MatOfPoint c : cnts) {
            MatOfPoint2f cont = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(cont, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(cont, approx, 0.02 * peri, true);

            if (approx.total() == 4) {
                displayCnt = new MatOfPoint(approx.toArray());
                break;
            }
        }
        cnts.clear();
        cnts.add(displayCnt);

        Imgproc.drawContours(orig, cnts, -1, new Scalar(255, 0, 0), -1);

//        Point sortedPoints[] = new Point[4];
//
//        MatOfPoint2f src = new MatOfPoint2f(
//                sortedPoints[0],
//                sortedPoints[1],
//                sortedPoints[2],
//                sortedPoints[3]);
//
//        MatOfPoint2f dst = new MatOfPoint2f(
//                new Point(0, 0),
//                new Point(450 - 1, 0),
//                new Point(0, 450 - 1),
//                new Point(450 - 1, 450 - 1)
//        );
//        Mat warpMat = Imgproc.getPerspectiveTransform(src,dst);
//        //This is you new image as Mat
//        Mat destImage = new Mat();
//        Imgproc.warpPerspective(orig, destImage, warpMat, orig.size());
        return orig;
    }

    public static Mat correctPerspective(Mat imgSource) {
        Imgproc.Canny(imgSource.clone(), imgSource, 50, 50);

        // apply gaussian blur to smoothen lines of dots
        Imgproc.GaussianBlur(imgSource, imgSource, new Size(5, 5), 5);

        // find the contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imgSource, contours, new

                Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = -1;
        MatOfPoint temp_contour = contours.get(0); // the largest is at the
        // index 0 for starting
        // point
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        for (
                int idx = 0; idx < contours.size(); idx++) {
            temp_contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(temp_contour);
            // compare this contour to the previous largest contour found
            if (contourarea > maxArea) {
                // check if this contour is a square
                MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                int contourSize = (int) temp_contour.total();
                MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
                Imgproc.approxPolyDP(new_mat, approxCurve_temp, contourSize * 0.05, true);
                if (approxCurve_temp.total() == 4) {
                    maxArea = contourarea;
                    approxCurve = approxCurve_temp;
                }
            }
        }

        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BayerBG2RGB);
        Mat sourceImage = imgSource;
        double[] temp_double;
        temp_double = approxCurve.get(0, 0);
        Point p1 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p1,55,new Scalar(0,0,255));
        // Imgproc.warpAffine(sourceImage, dummy, rotImage,sourceImage.size());
        temp_double = approxCurve.get(1, 0);
        Point p2 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p2,150,new Scalar(255,255,255));
        temp_double = approxCurve.get(2, 0);
        Point p3 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p3,200,new Scalar(255,0,0));
        temp_double = approxCurve.get(3, 0);
        Point p4 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p4,100,new Scalar(0,0,255));
        List<Point> source = new ArrayList<Point>();
        source.add(p1);
        source.add(p2);
        source.add(p3);
        source.add(p4);
        Mat startM = Converters.vector_Point2f_to_Mat(source);
        Mat result = warp(sourceImage, startM);

        return result;
    }

    public static Mat warp(Mat inputMat, Mat startM) {

        int resultWidth = 1200;
        int resultHeight = 680;

        Point ocvPOut4 = new Point(0, 0);
        Point ocvPOut1 = new Point(0, resultHeight);
        Point ocvPOut2 = new Point(resultWidth, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, 0);

        if (inputMat.height() > inputMat.width()) {
            // int temp = resultWidth;
            // resultWidth = resultHeight;
            // resultHeight = temp;

            ocvPOut3 = new Point(0, 0);
            ocvPOut4 = new Point(0, resultHeight);
            ocvPOut1 = new Point(resultWidth, resultHeight);
            ocvPOut2 = new Point(resultWidth, 0);
        }

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);

        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight), Imgproc.INTER_CUBIC);

        return outputMat;
    }


    private Mat imageProcessor(Mat rgba) {
        Mat temp = new Mat();
        Imgproc.cvtColor(rgba, temp, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(temp, temp, new Size(5, 5), 5);
        Imgproc.threshold(temp, temp, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(17, 3));
        Imgproc.morphologyEx(temp, temp, 3, element);
        Imgproc.cvtColor(temp, rgba, Imgproc.COLOR_GRAY2RGBA, 4);

        return temp;
    }
}